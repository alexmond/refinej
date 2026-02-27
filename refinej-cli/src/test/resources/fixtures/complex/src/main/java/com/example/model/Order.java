package com.example.model;

import java.util.List;
import java.util.ArrayList;

/**
 * Order entity that references User.
 */
public class Order extends Entity {

    private final User owner;
    private final List<String> items;

    public Order(Long id, User owner) {
        super(id);
        this.owner = owner;
        this.items = new ArrayList<>();
    }

    public User getOwner() {
        return this.owner;
    }

    public List<String> getItems() {
        return this.items;
    }

    public void addItem(String item) {
        this.items.add(item);
    }

}
