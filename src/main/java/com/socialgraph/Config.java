package com.socialgraph;

import io.github.cdimascio.dotenv.Dotenv;

public final class Config {
    private final String uri;
    private final String username;
    private final String password;
    private final String database;

    private Config(String uri, String username, String password, String database) {
        this.uri = uri;
        this.username = username;
        this.password = password;
        this.database = database;
    }

    public static Config load() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String uri = pick(dotenv, "NEO4J_URI");
        String user = pick(dotenv, "NEO4J_USERNAME", "NEO4J_USER");
        String pass = pick(dotenv, "NEO4J_PASSWORD");
        String db = pick(dotenv, "NEO4J_DATABASE");
        require("NEO4J_URI", uri);
        require("NEO4J_USERNAME", user);
        require("NEO4J_PASSWORD", pass);
        return new Config(uri, user, pass, db);
    }

    private static String pick(Dotenv dotenv, String... keys) {
        for (String key : keys) {
            String envValue = System.getenv(key);
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
            String dotenvValue = dotenv.get(key);
            if (dotenvValue != null && !dotenvValue.isBlank()) {
                return dotenvValue;
            }
        }
        return null;
    }

    private static void require(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Missing required environment variable: " + key
                + ". Copy .env.example to .env and populate it with your Neo4j credentials.");
        }
    }

    public String uri() { return uri; }
    public String username() { return username; }
    public String password() { return password; }
    public String database() { return database; }
}
