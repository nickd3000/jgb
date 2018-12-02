package com.physmo.jgb;

public interface ROMBank {
	public void poke(int address, int data);
	public int peek(int address);
}

