package com.socialgraph.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.mindrot.jbcrypt.BCrypt;

import com.socialgraph.DatabaseService;

/**
 * One-shot importer for the SNAP ego-Facebook dataset.
 *
 * Reconstructs the social graph by:
 *   1. Treating each ego node E as connected to every alter that appears in its
 *      .feat file (canonical SNAP convention - ego is implicitly connected to
 *      all alters in its ego-network).
 *   2. Adding alter-alter edges from each .edges file.
 * Friendships are deduplicated as unordered pairs and then materialised as TWO
 * directed FOLLOWS relationships (A->B and B->A), which preserves Facebook's
 * mutual-friendship semantics while satisfying the assignment's directed-edge
 * requirement.
 */
public final class DatasetLoader {

    private static final int USER_BATCH = 5_000;
    private static final int EDGE_BATCH = 5_000;

    /** Ego ids present in the SNAP ego-Facebook bundle. */
    private static final String[] EGO_IDS = {
        "0", "107", "348", "414", "686", "698", "1684", "1912", "3437", "3980"
    };

    /**
     * Pool of common given names and surnames used to give imported users a
     * recognisable display name. Picked deterministically from the SNAP id so
     * loads are reproducible.
     */
    private static final String[] FIRST_NAMES = {
        "Alex", "Sam", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Jamie",
        "Avery", "Quinn", "Drew", "Hayden", "Skyler", "Reese", "Rowan", "Sage",
        "Dakota", "Emerson", "Finley", "Harper", "Indigo", "Kai", "Logan",
        "Marlowe", "Nova", "Oakley", "Parker", "River", "Shiloh", "Tatum",
        "Blair", "Cameron", "Devon", "Ellis", "Frankie", "Gray", "Hollis",
        "Iris", "Jules", "Kendall", "Lane", "Max", "Nico", "Onyx", "Phoenix",
        "Quincy", "Robin", "Sloan", "Tyler", "Vesper"
    };
    private static final String[] LAST_NAMES = {
        "Chen", "Patel", "Rivera", "Nguyen", "Khan", "Williams", "Martinez",
        "Anderson", "Singh", "Garcia", "OConnor", "Brown", "Park", "Tanaka",
        "Silva", "Cohen", "Rossi", "Schmidt", "Dubois", "Hassan", "Petrov",
        "Lopez", "Wright", "Murphy", "Bennett", "Hughes", "Foster", "Bishop",
        "Holt", "Reed", "Pope", "Hayes", "Black", "Stone", "Lane", "Ford",
        "Wells", "Pierce", "Knight", "Spencer", "Carr", "Russell", "Pena",
        "Gomez", "Vargas", "Cruz", "Day", "Bauer", "Steiner", "Lambert"
    };

    private final DatabaseService db;
    private final Path dataDir;

    public DatasetLoader(DatabaseService db, Path dataDir) {
        this.db = db;
        this.dataDir = dataDir;
    }

    public Stats loadAll(boolean reset) throws IOException {
        if (reset) {
            wipeGraph();
        }
        db.ensureSchema();

        long t0 = System.nanoTime();
        Set<String> nodeIds = new HashSet<>();
        Set<Long> friendshipKeys = new HashSet<>();

        for (String egoId : EGO_IDS) {
            int egoInt = Integer.parseInt(egoId);
            nodeIds.add(egoId);

            List<Integer> alters = readAlterIds(dataDir.resolve(egoId + ".feat"));
            for (int alter : alters) {
                nodeIds.add(Integer.toString(alter));
                friendshipKeys.add(packPair(egoInt, alter));
            }

            Path edgesFile = dataDir.resolve(egoId + ".edges");
            try (Stream<String> lines = Files.lines(edgesFile, StandardCharsets.UTF_8)) {
                lines.forEach(line -> {
                    if (line.isBlank()) return;
                    int sep = line.indexOf(' ');
                    if (sep <= 0) return;
                    int a = Integer.parseInt(line.substring(0, sep).trim());
                    int b = Integer.parseInt(line.substring(sep + 1).trim());
                    if (a == b) return;
                    nodeIds.add(Integer.toString(a));
                    nodeIds.add(Integer.toString(b));
                    friendshipKeys.add(packPair(a, b));
                });
            }
        }

        long parsedAt = System.nanoTime();
        System.out.printf("Parsed dataset: %d nodes, %d unique friendships in %.1fs%n",
            nodeIds.size(), friendshipKeys.size(), (parsedAt - t0) / 1e9);

        long usersWritten = writeUsers(nodeIds);
        long followsWritten = writeFollows(friendshipKeys);

        long counted = countNodes();
        long countedRels = countFollows();
        return new Stats(nodeIds.size(), friendshipKeys.size(),
            usersWritten, followsWritten, counted, countedRels);
    }

    private void wipeGraph() {
        System.out.println("Wiping existing graph...");
        // Detach delete in chunks to avoid blowing up memory on big graphs.
        long deleted;
        do {
            var res = db.runEager("""
                MATCH (n)
                WITH n LIMIT 10000
                DETACH DELETE n
                """, Map.of());
            deleted = res.summary().counters().nodesDeleted();
        } while (deleted > 0);
    }

    private long writeUsers(Set<String> nodeIds) {
        String hash = BCrypt.hashpw("password123", BCrypt.gensalt(10));
        List<Map<String, Object>> batch = new ArrayList<>(USER_BATCH);
        long total = 0;
        long t0 = System.nanoTime();
        for (String id : nodeIds) {
            int idInt = Integer.parseInt(id);
            String first = FIRST_NAMES[(int) Math.floorMod(idInt * 2654435761L, FIRST_NAMES.length)];
            String last = LAST_NAMES[(int) Math.floorMod((long) idInt * 40503L + 17L, LAST_NAMES.length)];
            batch.add(Map.<String, Object>of(
                "id", id,
                "username", "user_" + id,
                "email", "user_" + id + "@example.com",
                "name", first + " " + last,
                "hash", hash
            ));
            if (batch.size() >= USER_BATCH) {
                total += flushUsers(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            total += flushUsers(batch);
            batch.clear();
        }
        System.out.printf("Wrote %d users in %.1fs%n", total, (System.nanoTime() - t0) / 1e9);
        return total;
    }

    private long flushUsers(List<Map<String, Object>> batch) {
        var res = db.runEager("""
            UNWIND $rows AS row
            MERGE (u:User {id: row.id})
            ON CREATE SET u.username = row.username,
                          u.email = row.email,
                          u.name = row.name,
                          u.bio = '',
                          u.passwordHash = row.hash,
                          u.createdAt = datetime()
            """, Map.of("rows", batch));
        return res.summary().counters().nodesCreated();
    }

    private long writeFollows(Set<Long> friendshipKeys) {
        List<Map<String, Object>> batch = new ArrayList<>(EDGE_BATCH);
        long total = 0;
        long t0 = System.nanoTime();
        for (long key : friendshipKeys) {
            int a = (int) (key >>> 32);
            int b = (int) (key & 0xFFFFFFFFL);
            batch.add(Map.<String, Object>of("a", Integer.toString(a), "b", Integer.toString(b)));
            if (batch.size() >= EDGE_BATCH) {
                total += flushFollows(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            total += flushFollows(batch);
            batch.clear();
        }
        System.out.printf("Wrote %d directed FOLLOWS edges in %.1fs%n",
            total, (System.nanoTime() - t0) / 1e9);
        return total;
    }

    private long flushFollows(List<Map<String, Object>> batch) {
        var res = db.runEager("""
            UNWIND $rows AS row
            MATCH (a:User {id: row.a}), (b:User {id: row.b})
            MERGE (a)-[r1:FOLLOWS]->(b) ON CREATE SET r1.since = datetime()
            MERGE (b)-[r2:FOLLOWS]->(a) ON CREATE SET r2.since = datetime()
            """, Map.of("rows", batch));
        return res.summary().counters().relationshipsCreated();
    }

    private long countNodes() {
        var rows = db.run("MATCH (u:User) RETURN count(u) AS c");
        return rows.isEmpty() ? 0 : rows.get(0).get("c").asLong();
    }

    private long countFollows() {
        var rows = db.run("MATCH ()-[r:FOLLOWS]->() RETURN count(r) AS c");
        return rows.isEmpty() ? 0 : rows.get(0).get("c").asLong();
    }

    /**
     * Read alter ids from a SNAP .feat file. Each line begins with the alter's
     * numeric id followed by the binary feature vector.
     */
    private static List<Integer> readAlterIds(Path featFile) throws IOException {
        if (!Files.exists(featFile)) return List.of();
        List<Integer> out = new ArrayList<>();
        try (Stream<String> lines = Files.lines(featFile, StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                if (line.isBlank()) return;
                int sep = line.indexOf(' ');
                String idTok = sep < 0 ? line : line.substring(0, sep);
                out.add(Integer.parseInt(idTok.trim()));
            });
        }
        return out;
    }

    private static long packPair(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }

    public record Stats(int parsedNodes, int parsedFriendships,
                        long usersCreated, long followsCreated,
                        long totalUsers, long totalFollows) {}
}
