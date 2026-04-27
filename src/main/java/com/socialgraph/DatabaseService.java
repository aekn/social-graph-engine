package com.socialgraph;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

public class DatabaseService implements AutoCloseable {
    private final Driver driver;

    public DatabaseService(String uri, String user, String password) {
       this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    @Override
    public void close() {
        driver.close();
    }

    // implement use cases
}
