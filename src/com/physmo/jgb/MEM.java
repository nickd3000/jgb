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
	
	public int RAM[] = new int[0x10000]; // 64k
	public int ROM[] = new int[0x10000]; // 64k
	public int CARTRIDGE[] = new int[0x10000]; // 64k
	
    // BIOS is active until the first instruction 0x00FF.
	public boolean biosActive = true;
	
	CPU cpu = null;
	
	public MEM(CPU cpu) {
		this.cpu = cpu;
	}
	
	public void poke(int addr, int val) {
		
		if (addr==CPU.ADDR_A) {
			cpu.A = val;
			return;
		}
		if (addr==CPU.ADDR_B) {
			cpu.B = val;
			return;
		}
		if (addr==CPU.ADDR_C) {
			cpu.C = val;
			return;
		}
		if (addr==CPU.ADDR_D) {
			cpu.D = val;
			return;
		}
		if (addr==CPU.ADDR_E) {
			cpu.E = val;
			return;
		}
		if (addr==CPU.ADDR_H) {
			cpu.H = val;
			return;
		}
		if (addr==CPU.ADDR_L) {
			cpu.L = val;
			return;
		}
		if (addr==CPU.ADDR_SP) {
			cpu.SP = val;
			return;
		}
		if (addr==CPU.ADDR_HL) {
			cpu.setHL(val);
			return;
		}
		if (addr==CPU.ADDR_DE) {
			cpu.setDE(val);
			return;
		}
		if (addr==CPU.ADDR_BC) {
			cpu.setBC(val);
			return;
		}
		
		RAM[addr]=val;
	}

	// TODO: need word versions of all these.
	
	public int peek(int addr) {
	
		if (addr==CPU.ADDR_A) {
			return cpu.A;
		}
		if (addr==CPU.ADDR_B) {
			return cpu.B;
		}
		if (addr==CPU.ADDR_C) {
			return cpu.C;
		}
		if (addr==CPU.ADDR_D) {
			return cpu.D;
		}
		if (addr==CPU.ADDR_E) {
			return cpu.E;
		}
		if (addr==CPU.ADDR_H) {
			return cpu.H;
		}
		if (addr==CPU.ADDR_L) {
			return cpu.L;
		}
		if (addr==CPU.ADDR_SP) {
			return cpu.SP;
		}
		if (addr==CPU.ADDR_HL) {
			return cpu.getHL();
		}
		if (addr==CPU.ADDR_DE) {
			return cpu.getDE();
		}
		if (addr==CPU.ADDR_BC) {
			return cpu.getBC();
		}
		
		// Boot rom.
		if (inRange(addr, 0, 0xff)) {
			if (biosActive) {
				return ROM[addr];
			} else
			{
				return CARTRIDGE[addr];
			}
		}
		
		// 0000   3FFF	16KB ROM bank 00				From cartridge, fixed bank
		if (inRange(addr,0x0000,0x3FFF)) 
		{
			return CARTRIDGE[addr];
		}
		
		// 4000   7FFF	16KB ROM Bank 01~NN				From cartridge, switchable bank via MBC (if any)
		if (inRange(addr,0x4000,0x7FFF))
		{
			return CARTRIDGE[addr]; 
		}
		
		
		
		 
		return RAM[addr];
	}

	public boolean inRange(int addr, int start, int end) {
		if (addr>=start && addr<=end) return true;
		return false;
	}
}
