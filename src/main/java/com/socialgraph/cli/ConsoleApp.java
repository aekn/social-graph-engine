package com.socialgraph.cli;

import java.io.Console;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import com.socialgraph.model.User;
import com.socialgraph.repository.GraphRepository;
import com.socialgraph.service.AuthService;
import com.socialgraph.service.SocialService;

public class ConsoleApp {
    private final AuthService auth;
    private final SocialService social;
    private final Scanner in;

    private User current;

    public ConsoleApp(AuthService auth, SocialService social) {
        this.auth = auth;
        this.social = social;
        this.in = new Scanner(System.in);
    }

    public void run() {
        printBanner();
        while (true) {
            if (current == null) {
                if (!preLoginMenu()) return;
            } else {
                if (!postLoginMenu()) return;
            }
        }
    }

    private void printBanner() {
        System.out.println();
        System.out.println("=========================================================");
        System.out.println(" Social Graph Engine - Neo4j-backed social network demo");
        System.out.println("=========================================================");
    }

    private boolean preLoginMenu() {
        System.out.println();
        System.out.println("--- Welcome ---");
        System.out.println(" 1. Register (UC-1)");
        System.out.println(" 2. Login (UC-2)");
        System.out.println(" 0. Quit");
        String choice = prompt("Choose: ");
        switch (choice) {
            case "1" -> doRegister();
            case "2" -> doLogin();
            case "0", "q", "quit", "exit" -> { return false; }
            default -> System.out.println("Unknown option.");
        }
        return true;
    }

    private boolean postLoginMenu() {
        System.out.println();
        System.out.println("--- Logged in as " + current.username() + " (id=" + current.id() + ") ---");
        System.out.println(" 1. View profile (UC-3)");
        System.out.println(" 2. Edit profile (UC-4)");
        System.out.println(" 3. Follow user (UC-5)");
        System.out.println(" 4. Unfollow user (UC-6)");
        System.out.println(" 5. View following / followers (UC-7)");
        System.out.println(" 6. Mutual connections (UC-8)");
        System.out.println(" 7. Friend recommendations (UC-9)");
        System.out.println(" 8. Search users (UC-10)");
        System.out.println(" 9. Popular users (UC-11)");
        System.out.println(" L. Logout");
        System.out.println(" 0. Quit");
        String choice = prompt("Choose: ");
        switch (choice.toLowerCase()) {
            case "1" -> doViewProfile();
            case "2" -> doEditProfile();
            case "3" -> doFollow();
            case "4" -> doUnfollow();
            case "5" -> doFollowingFollowers();
            case "6" -> doMutual();
            case "7" -> doRecommend();
            case "8" -> doSearch();
            case "9" -> doPopular();
            case "l", "logout" -> { current = null; System.out.println("Logged out."); }
            case "0", "q", "quit", "exit" -> { return false; }
            default -> System.out.println("Unknown option.");
        }
        return true;
    }

    private void doRegister() {
        System.out.println();
        System.out.println("[UC-1] Register a new account");
        String username = prompt("Username: ");
        String email = prompt("Email: ");
        String name = prompt("Full name: ");
        String password = promptPassword("Password: ");
        try {
            var result = auth.register(username, email, name, password);
            if (!result.ok()) {
                System.out.println("Registration failed: " + result.error());
                return;
            }
            System.out.println("Welcome, " + result.user().username() + "! You can now log in.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Registration failed: " + ex.getMessage());
        }
    }

    private void doLogin() {
        System.out.println();
        System.out.println("[UC-2] Login");
        String username = prompt("Username: ");
        String password = promptPassword("Password: ");
        Optional<User> user = auth.login(username, password);
        if (user.isEmpty()) {
            System.out.println("Invalid username or password.");
            return;
        }
        current = user.get();
        System.out.println("Logged in as " + current.username() + ".");
    }

    private void doViewProfile() {
        System.out.println();
        System.out.println("[UC-3] View profile");
        Optional<User> me = social.findById(current.id());
        if (me.isEmpty()) {
            System.out.println("Profile not found.");
            return;
        }
        current = me.get();
        printUserDetail(current);
    }

    private void doEditProfile() {
        System.out.println();
        System.out.println("[UC-4] Edit profile (leave blank to keep existing value)");
        printUserDetail(current);
        String newName = prompt("New name [" + current.name() + "]: ");
        String newBio = prompt("New bio [" + current.bio() + "]: ");
        String name = newName.isBlank() ? current.name() : newName;
        String bio = newBio.isBlank() ? current.bio() : newBio;
        User updated = social.updateProfile(current.id(), name, bio);
        current = updated;
        System.out.println("Profile updated.");
        printUserDetail(updated);
    }

    private void doFollow() {
        System.out.println();
        System.out.println("[UC-5] Follow another user");
        String token = prompt("Username or id to follow: ");
        Optional<User> target = social.resolveTarget(token);
        if (target.isEmpty()) {
            System.out.println("No such user.");
            return;
        }
        try {
            boolean created = social.follow(current.id(), target.get().id());
            System.out.println(created
                ? "You now follow " + target.get().username() + "."
                : "You already follow " + target.get().username() + ".");
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void doUnfollow() {
        System.out.println();
        System.out.println("[UC-6] Unfollow a user");
        String token = prompt("Username or id to unfollow: ");
        Optional<User> target = social.resolveTarget(token);
        if (target.isEmpty()) {
            System.out.println("No such user.");
            return;
        }
        boolean removed = social.unfollow(current.id(), target.get().id());
        System.out.println(removed
            ? "You no longer follow " + target.get().username() + "."
            : "You weren't following " + target.get().username() + ".");
    }

    private void doFollowingFollowers() {
        System.out.println();
        System.out.println("[UC-7] Following / Followers");
        System.out.println(" a. People I follow (following)");
        System.out.println(" b. People who follow me (followers)");
        String c = prompt("Choose [a/b]: ");
        if (c.equalsIgnoreCase("a")) {
            List<User> users = social.following(current.id());
            printUserList("Following (" + users.size() + ")", users);
        } else if (c.equalsIgnoreCase("b")) {
            List<User> users = social.followers(current.id());
            printUserList("Followers (" + users.size() + ")", users);
        } else {
            System.out.println("Unknown option.");
        }
    }

    private void doMutual() {
        System.out.println();
        System.out.println("[UC-8] Mutual connections");
        String token = prompt("Other user (username or id): ");
        Optional<User> other = social.resolveTarget(token);
        if (other.isEmpty()) {
            System.out.println("No such user.");
            return;
        }
        List<User> users = social.mutual(current.id(), other.get().id());
        printUserList("Mutual between " + current.username() + " and " + other.get().username()
            + " (" + users.size() + ")", users);
    }

    private void doRecommend() {
        System.out.println();
        System.out.println("[UC-9] Friend recommendations (you follow A, A follows B -> recommend B)");
        List<GraphRepository.Recommendation> recs = social.recommend(current.id(), 10);
        if (recs.isEmpty()) {
            System.out.println("No recommendations yet. Follow more people to see suggestions.");
            return;
        }
        System.out.println("Top " + recs.size() + " suggestions:");
        int i = 1;
        for (var r : recs) {
            System.out.printf("  %2d. %s  (%d common connections)%n",
                i++, r.user().displayLine(), r.commonConnections());
        }
    }

    private void doSearch() {
        System.out.println();
        System.out.println("[UC-10] Search users");
        String q = prompt("Search query (matches username or name): ");
        List<User> users = social.search(q);
        printUserList("Results for '" + q + "' (" + users.size() + ")", users);
    }

    private void doPopular() {
        System.out.println();
        System.out.println("[UC-11] Most-followed users");
        List<GraphRepository.PopularUser> pops = social.popular(10);
        if (pops.isEmpty()) {
            System.out.println("No follows in the graph yet.");
            return;
        }
        int i = 1;
        for (var p : pops) {
            System.out.printf("  %2d. %s  (%d followers)%n",
                i++, p.user().displayLine(), p.followers());
        }
    }

    private static void printUserDetail(User u) {
        System.out.println("  username: " + u.username());
        System.out.println("  id:       " + u.id());
        System.out.println("  name:     " + u.name());
        System.out.println("  email:    " + u.email());
        System.out.println("  bio:      " + (u.bio() == null || u.bio().isBlank() ? "(none)" : u.bio()));
    }

    private static void printUserList(String header, List<User> users) {
        System.out.println(header);
        if (users.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        int i = 1;
        for (User u : users) {
            System.out.printf("  %3d. %s%n", i++, u.displayLine());
        }
    }

    private String prompt(String label) {
        System.out.print(label);
        if (!in.hasNextLine()) return "";
        return in.nextLine().trim();
    }

    private String promptPassword(String label) {
        Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword(label);
            return pwd == null ? "" : new String(pwd);
        }
        // Fallback (e.g. running inside an IDE without a real TTY): visible input.
        System.out.print(label);
        if (!in.hasNextLine()) return "";
        return in.nextLine();
    }
}
