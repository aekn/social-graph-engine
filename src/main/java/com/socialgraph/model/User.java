package com.socialgraph.model;

public record User(String id, String username, String email, String name, String bio) {
    public String displayLine() {
        return "[" + username + "] " + (name == null ? "" : name) + "  (id=" + id + ")";
    }
}
