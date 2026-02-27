package com.example;

import com.example.model.Order;
import com.example.model.User;
import com.example.service.OrderService;
import com.example.service.UserService;

/**
 * Main application that wires together all services.
 */
public class App {

    public static void main(String[] args) {
        UserService userService = new UserService();
        OrderService orderService = new OrderService(userService);

        User alice = userService.createUser("Alice", "alice@example.com");
        User bob = userService.createUser("Bob", "bob@example.com");

        Order order = orderService.createOrder(alice.getId());
        order.addItem("Widget");
        order.addItem("Gadget");

        System.out.println("Order for " + order.getOwner().getName());
    }

}
