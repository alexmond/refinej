package com.example.simple;

/**
 * Fixture class used by EngineContractTest.
 */
public class Greeter {

    private final String name;

    public Greeter(String name) {
        this.name = name;
    }

    public String greet() {
        return "Hello, " + name + "!";
    }
}
