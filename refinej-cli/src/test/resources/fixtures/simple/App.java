package com.example.simple;

/**
 * Fixture class that uses Greeter — used to test reference extraction.
 */
public class App {

    public static void main(String[] args) {
        Greeter greeter = new Greeter("World");
        System.out.println(greeter.greet());
    }
}
