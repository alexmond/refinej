package com.example.model;

/**
 * Base entity class with an ID field.
 */
public abstract class Entity {

    private final Long id;

    protected Entity(Long id) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

}
