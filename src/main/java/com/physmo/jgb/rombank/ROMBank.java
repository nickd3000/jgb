package com.physmo.jgb;

public interface ROMBank {
	void poke(int address, int data);
	int peek(int address);
}

