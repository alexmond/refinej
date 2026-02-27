package com.example.model;

/**
 * User entity extending Entity.
 */
public class User extends Entity {

    private String name;
    private String email;

    public User(Long id, String name, String email) {
        super(id);
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return this.name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setName(String name) {
        this.name = name;
    }

}
