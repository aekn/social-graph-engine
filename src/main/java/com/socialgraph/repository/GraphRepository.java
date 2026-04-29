package com.socialgraph.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.Record;

import com.socialgraph.DatabaseService;
import com.socialgraph.model.User;

public class GraphRepository {
    private final DatabaseService db;

    public GraphRepository(DatabaseService db) {
        this.db = db;
    }

    public boolean follow(String myId, String targetId) {
        var summary = db.runEager("""
            MATCH (a:User {id:$me}), (b:User {id:$target})
            MERGE (a)-[r:FOLLOWS]->(b)
              ON CREATE SET r.since = datetime()
            RETURN r
            """, Map.of("me", myId, "target", targetId)).summary();
        return summary.counters().relationshipsCreated() > 0;
    }

    public boolean unfollow(String myId, String targetId) {
        var summary = db.runEager("""
            MATCH (:User {id:$me})-[r:FOLLOWS]->(:User {id:$target})
            DELETE r
            """, Map.of("me", myId, "target", targetId)).summary();
        return summary.counters().relationshipsDeleted() > 0;
    }

    public List<User> following(String myId) {
        return collectUsers(db.run("""
            MATCH (:User {id:$me})-[:FOLLOWS]->(u:User)
            RETURN u
            ORDER BY u.username
            """, Map.of("me", myId)));
    }

    public List<User> followers(String myId) {
        return collectUsers(db.run("""
            MATCH (:User {id:$me})<-[:FOLLOWS]-(u:User)
            RETURN u
            ORDER BY u.username
            """, Map.of("me", myId)));
    }

    public List<User> mutual(String myId, String otherId) {
        return collectUsers(db.run("""
            MATCH (:User {id:$me})-[:FOLLOWS]->(m:User)<-[:FOLLOWS]-(:User {id:$other})
            RETURN DISTINCT m AS u
            ORDER BY u.username
            """, Map.of("me", myId, "other", otherId)));
    }

    public List<Recommendation> recommend(String myId, int limit) {
        List<Record> records = db.run("""
            MATCH (me:User {id:$me})-[:FOLLOWS]->(:User)-[:FOLLOWS]->(rec:User)
            WHERE rec.id <> $me AND NOT (me)-[:FOLLOWS]->(rec)
            WITH rec, count(*) AS commonConnections
            RETURN rec AS u, commonConnections
            ORDER BY commonConnections DESC, u.username ASC
            LIMIT $limit
            """, Map.of("me", myId, "limit", limit));
        List<Recommendation> out = new ArrayList<>(records.size());
        for (Record r : records) {
            out.add(new Recommendation(
                UserRepository.toUser(r.get("u").asNode()),
                r.get("commonConnections").asLong()));
        }
        return out;
    }

    public List<PopularUser> popular(int limit) {
        List<Record> records = db.run("""
            MATCH (u:User)<-[:FOLLOWS]-()
            WITH u, count(*) AS followers
            RETURN u, followers
            ORDER BY followers DESC, u.username ASC
            LIMIT $limit
            """, Map.of("limit", limit));
        List<PopularUser> out = new ArrayList<>(records.size());
        for (Record r : records) {
            out.add(new PopularUser(
                UserRepository.toUser(r.get("u").asNode()),
                r.get("followers").asLong()));
        }
        return out;
    }

    private static List<User> collectUsers(List<Record> records) {
        List<User> users = new ArrayList<>(records.size());
        for (Record r : records) {
            users.add(UserRepository.toUser(r.get("u").asNode()));
        }
        return users;
    }

    public record Recommendation(User user, long commonConnections) {}
    public record PopularUser(User user, long followers) {}
}
