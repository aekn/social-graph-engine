package com.socialgraph;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.socialgraph.cli.ConsoleApp;
import com.socialgraph.loader.DatasetLoader;
import com.socialgraph.repository.GraphRepository;
import com.socialgraph.repository.UserRepository;
import com.socialgraph.service.AuthService;
import com.socialgraph.service.SocialService;

public class Main {
    public static void main(String[] args) throws Exception {
        List<String> argList = Arrays.asList(args);
        boolean load = argList.contains("--load");
        boolean reset = argList.contains("--reset");
        Path dataDir = parsePath(argList, "--data", Path.of("facebook"));

        Config config = Config.load();
        try (DatabaseService db = new DatabaseService(config)) {
            db.verifyConnectivity();
            String dbLabel = (config.database() == null || config.database().isBlank())
                ? "<server-default>"
                : config.database();
            System.out.println("Connected to Neo4j at " + config.uri()
                + " (database: " + dbLabel + ").");

            db.ensureSchema();

            if (load) {
                System.out.println("Loading SNAP ego-Facebook dataset from " + dataDir.toAbsolutePath()
                    + (reset ? " (resetting existing data)..." : " (preserving existing data)..."));
                DatasetLoader loader = new DatasetLoader(db, dataDir);
                DatasetLoader.Stats stats = loader.loadAll(reset);
                System.out.println();
                System.out.println("Load complete.");
                System.out.printf("  Parsed:   %d nodes, %d unique friendships%n",
                    stats.parsedNodes(), stats.parsedFriendships());
                System.out.printf("  Created:  %d users, %d FOLLOWS edges%n",
                    stats.usersCreated(), stats.followsCreated());
                System.out.printf("  In graph: %d users, %d FOLLOWS edges%n",
                    stats.totalUsers(), stats.totalFollows());
                return;
            }

            UserRepository userRepo = new UserRepository(db);
            GraphRepository graphRepo = new GraphRepository(db);
            AuthService auth = new AuthService(userRepo);
            SocialService social = new SocialService(userRepo, graphRepo);

            new ConsoleApp(auth, social).run();
        }
    }

    private static Path parsePath(List<String> args, String flag, Path fallback) {
        int i = args.indexOf(flag);
        if (i >= 0 && i + 1 < args.size()) {
            return Path.of(args.get(i + 1));
        }
        return fallback;
    }
}
