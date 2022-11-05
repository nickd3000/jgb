package com.physmo.jgb;

public class MemoryDegrader {

    // Experimental - methods to degrade memory over time.
    public static void degradeMemory(MEM mem) {

        // Vram
        if (Math.random() < 0.002) glitchArea(mem, 0x8000, 0x9fff);

        // OAM
        if (Math.random() < 0.002) glitchArea(mem, 0xFE00, 0xFE9F);

        // High RAM (HRAM)
        //if (Math.random()<0.02) glitchArea(0xFF80, 0xFFFE);

        // work ram bank 0
        if (Math.random() < 0.002) glitchArea(mem, 0xC000, 0xCFFF);
    }

    private static void glitchArea(MEM mem, int start, int end) {
        int addr = start + (int) (Math.random() * (end - start));
        int val = mem.peek(addr);
        val += (int) ((Math.random() - 0.5) * 3);
        val = val & 0xff;
        mem.poke(addr, val);
    }
}
