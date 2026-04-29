package com.socialgraph.service;

import java.util.Optional;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

import com.socialgraph.model.User;
import com.socialgraph.repository.UserRepository;

public class AuthService {
    private static final int BCRYPT_ROUNDS = 10;

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public RegisterResult register(String username, String email, String name, String password) {
        String u = required(username, "Username");
        String e = required(email, "Email");
        String n = required(name, "Name");
        String p = required(password, "Password");

        if (!u.matches("^[A-Za-z0-9_]{3,30}$")) {
            return RegisterResult.failure("Username must be 3-30 chars: letters, digits, underscores.");
        }
        if (!e.contains("@") || e.length() < 5) {
            return RegisterResult.failure("Email looks invalid.");
        }
        if (p.length() < 6) {
            return RegisterResult.failure("Password must be at least 6 characters.");
        }
        if (users.usernameExists(u)) {
            return RegisterResult.failure("Username already taken.");
        }
        if (users.emailExists(e)) {
            return RegisterResult.failure("Email already in use.");
        }

        String hash = BCrypt.hashpw(p, BCrypt.gensalt(BCRYPT_ROUNDS));
        String id = UUID.randomUUID().toString();
        User user = users.create(id, u, e, n, hash);
        return RegisterResult.success(user);
    }

    public Optional<User> login(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        var creds = users.findCredentials(username.trim());
        if (creds.isEmpty()) return Optional.empty();
        String hash = creds.get().passwordHash();
        if (hash == null || hash.isBlank()) return Optional.empty();
        if (!BCrypt.checkpw(password, hash)) return Optional.empty();
        return users.findById(creds.get().userId());
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    public record RegisterResult(boolean ok, String error, User user) {
        public static RegisterResult success(User u) { return new RegisterResult(true, null, u); }
        public static RegisterResult failure(String err) { return new RegisterResult(false, err, null); }
    }
}
