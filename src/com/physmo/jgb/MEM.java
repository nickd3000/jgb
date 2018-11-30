package com.physmo.jgb;

/*
 * 
 * Start  End   Description						Notes
 * 0000   3FFF	16KB ROM bank 00				From cartridge, fixed bank
 * 4000   7FFF	16KB ROM Bank 01~NN				From cartridge, switchable bank via MBC (if any)
 * 8000   9FFF	8KB Video RAM (VRAM)			Only bank 0 in Non-CGB mode / Switchable bank 0/1 in CGB mode
 * A000   BFFF	8KB External RAM				In cartridge, switchable bank if any
 * C000   CFFF	4KB Work RAM (WRAM) bank 0	
 * D000   DFFF	4KB Work RAM (WRAM) bank 1~N	Only bank 1 in Non-CGB mode / Switchable bank 1~7 in CGB mode
 * E000   FDFF	Mirror of C000~DDFF (ECHO RAM)	Typically not used
 * FE00   FE9F	Sprite attribute table (OAM)	
 * FEA0   FEFF	Not Usable	
 * FF00   FF7F	I/O Registers	
 * FF80   FFFE	High RAM (HRAM)	
 * FFFF   FFFF	Interrupts Enable Register (IE)	
 * 
 * 
 */

public class MEM {

	ROMBank memoryBank = null;

	public int RAM[] = new int[0x10000]; // 64k
	public int RAM_BANKS[] = new int[0x10000]; // 64k
	public int BIOS[] = new int[0x10000]; // 64k
	public int CARTRIDGE[] = new int[0x10000 * 100]; // 64k

	// BIOS is active until the first instruction 0x00FF.
	public boolean biosActive = true;

	CPU cpu = null;

	public MEM(CPU cpu) {
		this.cpu = cpu;
		memoryBank = new MBC1(cpu);
	}

	public void writeBigMessage(String msg, int count) {
		for (int i = 0; i < count; i++) {
			System.out.println(msg);
		}
	}

	public void poke(int addr, int val) {
		
		if (addr < 0) {
			pokeSpecial(addr, val);
			return;
		}
		
		if (isSwitchableddress(addr)) {
			memoryBank.poke(addr, val);
			return;
		}
		
		// Rough serial output.
		if (addr == 0xFF02 && val == 0x81) {
			System.out.println("SERIAL:  "+(char)RAM[0xFF01]);
		}
		
		if (addr == 0xFF43) {
			//writeBigMessage("DEBUG: Poked "+Utils.toHex4(addr)+"  val="+val, 1);
		}
		
		if (addr == val) {
			writeBigMessage("poke addr==val!!", 100000);
		}

		if (addr < 0x8000) {
			memoryBank.poke(addr, val);
		}

		if (addr == 0xFF00) {
			cpu.input.pokeFF00(val);
			return;
		}

		// Writing anything to the scanline register resets it.
		if (addr == 0xFF44) {
			RAM[0xFF44] = 0;
		}

		// Writing 1 to this address switches the bios out.
		if (addr == 0xFF50 && val == 1) {
			// RAM[0xFF50]=1;
			biosActive = false;
			writeBigMessage("Switched bios!!!", 10);
		}

		if (addr == 0x0151) {
			writeBigMessage("wrote to 0x0150!!!", 1000);
		}

		if (addr == CPU.ADDR_FF46_DMA_TRANSFER) {
			// RAM[addr] = val;
			transferDMA(val);
			return;
		}

		if (addr == CPU.ADDR_FF44_Y_SCANLINE) {
			RAM[addr] = 1;
			return;
		}



		// this area is restricted
		if ((addr >= 0xFEA0) && (addr < 0xFEFF)) {
		}

//		if (inRange(addr, 0x0000, 0x7FFF)) {
//
//			for (int i = 0; i < 10; i++) {
//				System.out.println(" bank switch attempt [addr 0x" + Utils.toHex4(addr) + " value " + Utils.toHex2(val)
//						+ "] --------------------");
//			}
//		}

		RAM[addr] = val;
	}

	// Return true if this is a memor address handled by the memory controller.
	// Switchable address range:
	// 0x4000 - 0x7FFF (16,384 bytes) Cartridge ROM Bank n 
	// 0xA000 - 0xBFFF (8,192 bytes) External RAM
	public boolean isSwitchableddress(int address) {
		if (address>=0x4000 && address<=0x7FFF) {
			return true;
		}
		if (address>=0xA000 && address<=0xBFFF) {
			return true;
		}	
		return false;
	}
	
	public int peek(int addr) {

		// INPUT
		if (addr == 0xFF00) {
			return cpu.input.peekFF00();
		}

		// Handle special negative addresses (registers)
		if (addr < 0) {
			return peekSpecial(addr);
		}

		if (isSwitchableddress(addr)) {
			return memoryBank.peek(addr);
		}
		
		// Boot rom.
		if (inRange(addr, 0, 0xff)) {
			if (biosActive) {
				return BIOS[addr];
			} else {
				return CARTRIDGE[addr];
			}
		}

		// LCD stat register
		if (addr == 0xFF41) {
			return RAM[0xFF41] |= 0x80;
		}

		// Scanline returns 0 if LCD is off
		if (addr == 0xFF44) {
			// TODO:
//			if ((RAM[0xFF40] & 0x80) == 0) {
//				return 0;
//			}
			return RAM[0xFF44];
		}

		// Are we reading from the switchable ROM memory bank?
		if (inRange(addr, 0x4000, 0x7FFF)) {
			return memoryBank.peek(addr);
		}

		// 0000 3FFF 16KB ROM bank 00 From cartridge, fixed bank
		if (inRange(addr, 0x0000, 0x3FFF)) {
			return CARTRIDGE[addr];
		}

		// * 8000 9FFF 8KB Video RAM (VRAM) Only bank 0 in Non-CGB mode / Switchable
		// bank 0/1 in CGB mode
		if (inRange(addr, 0x8000, 0x9FFF)) {
			return RAM[addr];
		}

		// * A000 BFFF 8KB External RAM In cartridge, switchable bank if any
		if (inRange(addr, 0xA000, 0xBFFF)) {
			return CARTRIDGE[addr];
		}

		// * C000 CFFF 4KB Work RAM (WRAM) bank 0
		if (inRange(addr, 0xC000, 0xCFFF)) {
			return RAM[addr];
		}

		// * D000 DFFF 4KB Work RAM (WRAM) bank 1~N Only bank 1 in Non-CGB mode /
		// Switchable bank 1~7 in CGB mode
		if (inRange(addr, 0xD000, 0xDFFF)) {
			return RAM[addr];
		}

		// * E000 FDFF Mirror of C000~DDFF (ECHO RAM) Typically not used
		// * FE00 FE9F Sprite attribute table (OAM)
		if (inRange(addr, 0xFE00, 0xFE9F)) {
			return RAM[addr];
		}
		// * FEA0 FEFF Not Usable
		// * FF00 FF7F I/O Registers

		// * FF80 FFFE High RAM (HRAM)
		if (inRange(addr, 0xFF80, 0xFFFE)) {
			return RAM[addr];
		}

		// * FFFF FFFF Interrupts Enable Register (IE)

		// return RAM[addr];
		return RAM[addr];
	}

	public boolean inRange(int addr, int start, int end) {
		if (addr >= start && addr <= end)
			return true;
		return false;
	}

	public void transferDMA(int val) {
		// int addr = peek(CPU.ADDR_FF46_DMA_TRANSFER) << 8;
		int addr = val << 8;
		for (int i = 0; i < 0xA0; i++) {
			poke(0xFE00 + i, peek(addr + i));
		}
	}

	// Handle register addresses (where Address<0)

	private void pokeSpecial(int addr, int val) {
		// TODO: make this use a switch statement.
		
		if (addr == CPU.ADDR_A) {
			cpu.A = val;
			return;
		}
		if (addr == CPU.ADDR_B) {
			cpu.B = val;
			return;
		}
		if (addr == CPU.ADDR_C) {
			cpu.C = val;
			return;
		}
		if (addr == CPU.ADDR_D) {
			cpu.D = val;
			return;
		}
		if (addr == CPU.ADDR_E) {
			cpu.E = val;
			return;
		}
		if (addr == CPU.ADDR_H) {
			cpu.H = val;
			return;
		}
		if (addr == CPU.ADDR_L) {
			cpu.L = val;
			return;
		}
		if (addr == CPU.ADDR_SP) {
			cpu.SP = val;
			cpu.topOfSTack=val;
			return;
		}
		if (addr == CPU.ADDR_HL) {
			cpu.setHL(val);
			return;
		}
		if (addr == CPU.ADDR_DE) {
			cpu.setDE(val);
			return;
		}
		if (addr == CPU.ADDR_BC) {
			cpu.setBC(val);
			return;
		}
		if (addr == CPU.ADDR_AF) {
			cpu.setAF(val);
			return;
		}

	}

	public int peekSpecial(int addr) {

		if (addr == CPU.ADDR_A) {
			return cpu.A;
		}
		if (addr == CPU.ADDR_B) {
			return cpu.B;
		}
		if (addr == CPU.ADDR_C) {
			return cpu.C;
		}
		if (addr == CPU.ADDR_D) {
			return cpu.D;
		}
		if (addr == CPU.ADDR_E) {
			return cpu.E;
		}
		if (addr == CPU.ADDR_H) {
			return cpu.H;
		}
		if (addr == CPU.ADDR_L) {
			return cpu.L;
		}
		if (addr == CPU.ADDR_SP) {
			return cpu.SP;
		}
		if (addr == CPU.ADDR_HL) {
			return cpu.getHL();
		}
		if (addr == CPU.ADDR_DE) {
			return cpu.getDE();
		}
		if (addr == CPU.ADDR_BC) {
			return cpu.getBC();
		}
		if (addr == CPU.ADDR_AF) {
			return cpu.getAF();
		}

		System.out.println("Error, unrecognised register address.");
		return 0;
	}
}
