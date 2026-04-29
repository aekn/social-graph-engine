package com.socialgraph;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.EagerResult;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.QueryConfig;
import org.neo4j.driver.Record;

import java.util.List;
import java.util.Map;

public class DatabaseService implements AutoCloseable {
    private final Driver driver;
    private final String database;

    public DatabaseService(Config config) {
        this.driver = GraphDatabase.driver(
            config.uri(),
            AuthTokens.basic(config.username(), config.password()));
        this.database = config.database();
    }

    public void verifyConnectivity() {
        driver.verifyConnectivity();
    }

    public Driver driver() {
        return driver;
    }

    public String database() {
        return database;
    }

    public List<Record> run(String cypher) {
        return run(cypher, Map.of());
    }

    public List<Record> run(String cypher, Map<String, Object> params) {
        return runEager(cypher, params).records();
    }

    public EagerResult runEager(String cypher) {
        return runEager(cypher, Map.of());
    }

    public EagerResult runEager(String cypher, Map<String, Object> params) {
        var query = driver.executableQuery(cypher)
            .withParameters(params);
        if (database != null && !database.isBlank()) {
            query = query.withConfig(QueryConfig.builder().withDatabase(database).build());
        }
        return query.execute();
    }

    public void ensureSchema() {
        runEager("CREATE CONSTRAINT user_id IF NOT EXISTS FOR (u:User) REQUIRE u.id IS UNIQUE");
        runEager("CREATE CONSTRAINT user_username IF NOT EXISTS FOR (u:User) REQUIRE u.username IS UNIQUE");
        runEager("CREATE CONSTRAINT user_email IF NOT EXISTS FOR (u:User) REQUIRE u.email IS UNIQUE");
        runEager("CREATE INDEX user_name IF NOT EXISTS FOR (u:User) ON (u.name)");
    }

    public void wipeAllData() {
        // Batched delete to keep transactions small on Aura Free.
        long deleted;
        do {
            EagerResult result = runEager(
                "MATCH (n) WITH n LIMIT 5000 DETACH DELETE n RETURN count(*) AS deleted");
            deleted = result.records().get(0).get("deleted").asLong();
        } while (deleted > 0);
    }

    @Override
    public void close() {
        driver.close();
    }
}
