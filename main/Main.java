package main;

import java.nio.file.Path;

import filter.Filter;

public class Main {

	// for testing
	public static void main(String[] args) {
		Path path = Path.of("..", "sandbox", "Images");
		Filter f = new Filter(path);
//		Filter f = new Filter(null);
	}

}
