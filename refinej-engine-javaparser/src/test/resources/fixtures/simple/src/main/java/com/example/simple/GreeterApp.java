package com.example.simple;

/**
 * Simple app that uses {@link Greeter} — generates type references and method calls for
 * indexing tests.
 */
public class GreeterApp {

	public static void main(String[] args) {
		Greeter greeter = new Greeter("World");
		System.out.println(greeter.greet());
	}

}
