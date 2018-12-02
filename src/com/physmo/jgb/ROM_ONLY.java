package com.physmo.jgb;

public class ROM_ONLY implements ROMBank {

	public CPU cpu;

	public ROM_ONLY(CPU cpu) {
		this.cpu = cpu;
		// this.mem = cpu.mem;
	}
	
	@Override
	public void poke(int address, int data) {
		if (address <= 0x7FFF) {
			//handleBankChange(address, data);
		}
		else if ((address >= 0xA000) && (address <= 0xBFFF)) {

			cpu.mem.RAM[address] = data;
			
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
			return cpu.mem.CARTRIDGE[address];
		}

		// Are we reading from the cartridge RAM memory bank?
		if ((address >= 0xA000) && (address <= 0xBFFF)) {
			return cpu.mem.RAM[address];
		}

		// else return memory
		return cpu.mem.RAM[address];
		//return cpu.mem.CARTRIDGE[address];
		
	}

}
