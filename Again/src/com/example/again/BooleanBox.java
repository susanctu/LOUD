package com.example.again;

public class BooleanBox {
	private boolean val;
	
	public BooleanBox(boolean b) {
		val = b;
	}

	public void set(boolean b) {
		val = b;
	}
	
	public boolean get() {
		return val;
	}
}
