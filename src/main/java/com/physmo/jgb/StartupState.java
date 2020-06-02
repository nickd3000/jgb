package com.physmo.jgb;

public class StartupState {

    /**
     * Sets the state of the machine to the state after the initial bios program has run.
     * This enables the machine to run as normal without a BIOS file, however no
     * startup logo will be displayed.
     */
    public static void setStartupState(CPU cpu, MEM mem) {
        cpu.setAF(0x01B0);
        cpu.setBC(0x0013);
        cpu.setDE(0x00D8);
        cpu.setHL(0x014D);
        cpu.SP = 0xFFFE;

        mem.poke(0xFF05, 0x00); //   ; TIMA
        mem.poke(0xFF06, 0x00); //   ; TMA
        mem.poke(0xFF07, 0x00); //   ; TAC
        mem.poke(0xFF10, 0x80); //   ; NR10
        mem.poke(0xFF11, 0xBF); //   ; NR11
        mem.poke(0xFF12, 0xF3); //   ; NR12
        mem.poke(0xFF14, 0xBF); //   ; NR14
        mem.poke(0xFF16, 0x3F); //   ; NR21
        mem.poke(0xFF17, 0x00); //   ; NR22
        mem.poke(0xFF19, 0xBF); //   ; NR24
        mem.poke(0xFF1A, 0x7F); //   ; NR30
        mem.poke(0xFF1B, 0xFF); // v   ; NR31
        mem.poke(0xFF1C, 0x9F); // v   ; NR32
        mem.poke(0xFF1E, 0xBF); //   ; NR33
        mem.poke(0xFF20, 0xFF); //   ; NR41
        mem.poke(0xFF21, 0x00); //   ; NR42
        mem.poke(0xFF22, 0x00); //   ; NR43
        mem.poke(0xFF23, 0xBF); //   ; NR30
        mem.poke(0xFF24, 0x77); //   ; NR50
        mem.poke(0xFF25, 0xF3); //   ; NR51
        mem.poke(0xFF26, 0xF1); //-GB, $F0-SGB ; NR52
        mem.poke(0xFF40, 0x91); //   ; LCDC
        mem.poke(0xFF42, 0x00); //   ; SCY
        mem.poke(0xFF43, 0x00); //   ; SCX
        mem.poke(0xFF45, 0x00); //   ; LYC
        mem.poke(0xFF47, 0xFC); //   ; BGP
        mem.poke(0xFF48, 0xFF); //   ; OBP0
        mem.poke(0xFF49, 0xFF); //   ; OBP1
        mem.poke(0xFF4A, 0x00); //   ; WY
        mem.poke(0xFF4B, 0x00); //   ; WX
        mem.poke(0xFFFF, 0x00); //   ; IE
    }

}
