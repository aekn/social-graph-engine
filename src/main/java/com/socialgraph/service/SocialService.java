package com.socialgraph.service;

import java.util.List;
import java.util.Optional;

import com.socialgraph.model.User;
import com.socialgraph.repository.GraphRepository;
import com.socialgraph.repository.UserRepository;

public class SocialService {
    private final UserRepository users;
    private final GraphRepository graph;

    public SocialService(UserRepository users, GraphRepository graph) {
        this.users = users;
        this.graph = graph;
    }

    public Optional<User> findById(String id) {
        return users.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return users.findByUsername(username);
    }

    public Optional<User> resolveTarget(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        String token = input.trim();
        Optional<User> byUsername = users.findByUsername(token);
        if (byUsername.isPresent()) return byUsername;
        return users.findById(token);
    }

    public User updateProfile(String userId, String name, String bio) {
        return users.updateProfile(userId, name, bio);
    }

    public List<User> search(String query) {
        return users.search(query);
    }

    public boolean follow(String myId, String targetId) {
        if (myId.equals(targetId)) {
            throw new IllegalArgumentException("You cannot follow yourself.");
        }
        return graph.follow(myId, targetId);
    }

    public boolean unfollow(String myId, String targetId) {
        return graph.unfollow(myId, targetId);
    }

    public List<User> following(String myId) {
        return graph.following(myId);
    }

    public List<User> followers(String myId) {
        return graph.followers(myId);
    }

    public List<User> mutual(String myId, String otherId) {
        return graph.mutual(myId, otherId);
    }

    public List<GraphRepository.Recommendation> recommend(String myId, int limit) {
        return graph.recommend(myId, limit);
    }

    public List<GraphRepository.PopularUser> popular(int limit) {
        return graph.popular(limit);
    }
}
