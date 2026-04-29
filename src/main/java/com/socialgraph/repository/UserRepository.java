package com.socialgraph.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;

import com.socialgraph.DatabaseService;
import com.socialgraph.model.User;

public class UserRepository {
    private final DatabaseService db;

    public UserRepository(DatabaseService db) {
        this.db = db;
    }

    public Optional<User> findByUsername(String username) {
        List<Record> records = db.run(
            "MATCH (u:User {username:$username}) RETURN u",
            Map.of("username", username));
        return records.isEmpty() ? Optional.empty() : Optional.of(toUser(records.get(0).get("u").asNode()));
    }

    public Optional<User> findById(String id) {
        List<Record> records = db.run(
            "MATCH (u:User {id:$id}) RETURN u",
            Map.of("id", id));
        return records.isEmpty() ? Optional.empty() : Optional.of(toUser(records.get(0).get("u").asNode()));
    }

    public Optional<UserCredentials> findCredentials(String username) {
        List<Record> records = db.run(
            "MATCH (u:User {username:$username}) RETURN u.id AS id, u.passwordHash AS hash",
            Map.of("username", username));
        if (records.isEmpty()) return Optional.empty();
        Record r = records.get(0);
        return Optional.of(new UserCredentials(
            r.get("id").asString(),
            r.get("hash").asString()));
    }

    public User create(String id, String username, String email, String name, String passwordHash) {
        List<Record> records = db.run("""
            CREATE (u:User {
                id: $id,
                username: $username,
                email: $email,
                name: $name,
                bio: '',
                passwordHash: $hash,
                createdAt: datetime()
            })
            RETURN u
            """, Map.of(
                "id", id,
                "username", username,
                "email", email,
                "name", name,
                "hash", passwordHash));
        return toUser(records.get(0).get("u").asNode());
    }

    public User updateProfile(String id, String name, String bio) {
        List<Record> records = db.run(
            "MATCH (u:User {id:$id}) SET u.name=$name, u.bio=$bio RETURN u",
            Map.of("id", id, "name", name, "bio", bio));
        if (records.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        return toUser(records.get(0).get("u").asNode());
    }

    public List<User> search(String query) {
        List<Record> records = db.run("""
            MATCH (u:User)
            WHERE toLower(u.username) CONTAINS toLower($q)
               OR toLower(u.name) CONTAINS toLower($q)
            RETURN u
            ORDER BY u.username
            LIMIT 20
            """, Map.of("q", query));
        List<User> users = new ArrayList<>(records.size());
        for (Record r : records) {
            users.add(toUser(r.get("u").asNode()));
        }
        return users;
    }

    public boolean usernameExists(String username) {
        return !db.run(
            "MATCH (u:User {username:$username}) RETURN u.id LIMIT 1",
            Map.of("username", username)).isEmpty();
    }

    public boolean emailExists(String email) {
        return !db.run(
            "MATCH (u:User {email:$email}) RETURN u.id LIMIT 1",
            Map.of("email", email)).isEmpty();
    }

    static User toUser(Node node) {
        return new User(
            stringOrEmpty(node.get("id")),
            stringOrEmpty(node.get("username")),
            stringOrEmpty(node.get("email")),
            stringOrEmpty(node.get("name")),
            stringOrEmpty(node.get("bio")));
    }

    private static String stringOrEmpty(Value v) {
        return v == null || v.isNull() ? "" : v.asString();
    }

    public record UserCredentials(String userId, String passwordHash) {}
}
