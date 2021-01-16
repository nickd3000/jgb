package com.physmo.base;

public interface MEM {
    void pokeWord(int addr, int val);

    void poke(int addr, int val);

    int peek(int addr);

    void debugPeek(int addr);

    void debugPoke(int addr, int val);
}
