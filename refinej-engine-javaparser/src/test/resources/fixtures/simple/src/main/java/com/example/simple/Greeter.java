package com.example.simple;

/**
 * Simple greeting class used as a test fixture.
 */
public class Greeter {

	private final String name;

	public Greeter(String name) {
		this.name = name;
	}

	public String greet() {
		return "Hello, " + this.name + "!";
	}

}
