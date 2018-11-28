package com.physmo.jgb;

public interface ROMBank {
	public void poke(int addr, int val);
	public int peek(int addr);
}

