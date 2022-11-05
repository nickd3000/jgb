package com.physmo.jgb.rombank;

import com.physmo.jgb.CPU;
import com.physmo.jgb.MEM;

public class ROM_ONLY implements ROMBank {

    public CPU cpu;
    private final MEM mem;

    public ROM_ONLY(CPU cpu) {
        this.cpu = cpu;
        mem = cpu.getMem();
    }

    @Override
    public void poke(int address, int data) {
        if (address <= 0x7FFF) {
            //handleBankChange(address, data);
        } else if ((address >= 0xA000) && (address <= 0xBFFF)) {

            mem.RAM[address] = data;

        }
    }

    @Override
    public int peek(int address) {

        // 0x4000 - 0x7FFF (16,384 bytes) Cartridge ROM Bank n
        // 0xA000 - 0xBFFF (8,192 bytes) External RAM
        // int newAddress = addr - 0x4000;
        // return cpu.mem.CARTRIDGE[newAddress + (currentRomBank * 0x4000)];

        // Are we reading from the SWITCHABLE ROM cartridge memory bank?
        if ((address >= 0x4000) && (address <= 0x7FFF)) {
            return mem.CARTRIDGE[address];
        }

        // Are we reading from the cartridge RAM memory bank?
        if ((address >= 0xA000) && (address <= 0xBFFF)) {
            return mem.RAM[address];
        }

        // else return memory
        return mem.RAM[address];
        //return cpu.mem.CARTRIDGE[address];

    }

}
