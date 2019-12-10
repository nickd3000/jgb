package com.physmo.jgb.rombank;

public interface ROMBank {
	void poke(int address, int data);
	int peek(int address);
}

