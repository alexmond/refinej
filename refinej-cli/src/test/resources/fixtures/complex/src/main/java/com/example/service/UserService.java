package com.example.service;

import com.example.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing users.
 */
public class UserService {

    private final Map<Long, User> users = new HashMap<>();

    public User createUser(String name, String email) {
        long id = this.users.size() + 1;
        User user = new User(id, name, email);
        this.users.put(id, user);
        return user;
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(this.users.get(id));
    }

    public void updateName(Long userId, String newName) {
        findById(userId).ifPresent(u -> u.setName(newName));
    }

}
