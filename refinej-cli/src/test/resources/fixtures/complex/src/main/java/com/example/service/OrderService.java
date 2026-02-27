package com.example.service;

import com.example.model.Order;
import com.example.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing orders. Depends on UserService.
 */
public class OrderService {

    private final UserService userService;
    private final List<Order> orders = new ArrayList<>();

    public OrderService(UserService userService) {
        this.userService = userService;
    }

    public Order createOrder(Long userId) {
        User user = this.userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        long id = this.orders.size() + 1;
        Order order = new Order(id, user);
        this.orders.add(order);
        return order;
    }

    public List<Order> getOrdersForUser(Long userId) {
        return this.orders.stream()
                .filter(o -> o.getOwner().getId().equals(userId))
                .toList();
    }

}
