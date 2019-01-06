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

	public static final int ADDR_FF01_SERIAL_DATA = 0xFF01;
	public static final int ADDR_FF46_DMA_TRANSFER = 0xFF46;// // FF46 (w) DM Transfer & Start Address
	public static final int ADDR_FF45_Y_COMPARE = 0xFF45; // lyc: 0, // $FF45 (r/w) LY Compare
	public static final int ADDR_FF44_Y_SCANLINE = 0xFF44; // 0xFF44
	public static final int ADDR_FF41_LCD_STAT = 0xFF41; // LCD status register FF41

	public static final int ADDR_0xFF47_BGPALETTE = 0xFF47;
	public static final int ADDR_0xFF48_SPRITEPALETTE1 = 0xFF48;
	public static final int ADDR_0xFF49_SPRITEPALETTE2 = 0xFF49;
	public static final int ADDR_0xFF44_SCANLINE = 0xFF44;
	 
	public static final int ADDR_0xFF4F_VRAMBANK = 0xFF4F; // FF4F - VBK - CGB Mode Only - VRAM Bank (R/W)
	
	public static final int ADDR_0xFF68_BGPALETTEINDEX = 0xFF68; // FF68 - BCPS/BGPI - CGB Mode Only - Background Palette Index
	public static final int ADDR_0xFF69_BGPALETTEDATA = 0xFF69; // FF69 - BCPD/BGPD - CGB Mode Only - Background Palette Data

	public static final int ADDR_0xFF6A_SPRITEPALETTEINDEX = 0xFF6A; // FF6A - OCPS/OBPI - CGB Mode Only - Sprite Palette Index
	public static final int ADDR_0xFF6B_SPRITEPALETTEDATA = 0xFF6B; // FF6B - OCPD/OBPD - CGB Mode Only - Sprite Palette Data


	
	ROMBank memoryBank = null;

	public int RAM[] = new int[0x10000]; // 64k
	public int RAM_BANKS1[] = new int[0x20000]; // switchable ram bank 1
	
	public int CART_RAM_BANKS[] = new int[0x10000]; // 64k
	public int BIOS[] = new int[0x10000]; // 64k
	public int CARTRIDGE[] = new int[0x10000 * 100]; // 64k
	public int VRAMBANK0[] = new int[0x1FFF]; // 8000 - 9FFF
	public int VRAMBANK1[] = new int[0x1FFF]; // 8000 - 9FFF

	// BIOS is active until the first instruction 0x00FF.
	public boolean biosActive = true;

	CPU cpu = null;
	
	int forceMemoryBankType = -1;

	public MEM(CPU cpu) {
		this.cpu = cpu;
	}
	
	public void init() {
		createMBCForHeaderType();
	}

	public void createMBCForHeaderType() {
		int mbcType = HEADER.getMemoryBankControllerType(cpu);
		
		if (forceMemoryBankType!=-1) mbcType=forceMemoryBankType;
		
		if (mbcType==0)
			memoryBank = new ROM_ONLY(cpu);
		else if (mbcType==1)
			memoryBank = new MBC1(cpu);
		else if (mbcType==0x1B || mbcType==0x19)
			memoryBank = new MBC5(cpu);
		else 
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
		
		// Writing to the divider register sets it to zero.
		if (addr == 0xFF04) {
		   RAM[0xFF04] = 0 ;
		}
		
		// Input.
		if (addr == 0xFF00) {
			cpu.input.pokeFF00(val);
			return;
		}
		
		if (isSwitchableddress(addr)) {
			memoryBank.poke(addr, val);
			//return;
		}
		
		// Rough serial output.
		if (addr == 0xFF02 && val == 0x81) {
			System.out.println("SERIAL:  "+(char)RAM[0xFF01]);
		}
		
		if (addr == 0xFF43) {
			//writeBigMessage("DEBUG: Poked "+Utils.toHex4(addr)+"  val="+val, 1);
		}
		
//		if (addr == val) {
//			writeBigMessage("poke addr==val!!", 100000);
//		}

		if (addr < 0x8000) {
			memoryBank.poke(addr, val);
		}


		// VRAM
		if (addr>=0x8000 && addr<0x9FFF) {
			if (cpu.hardwareType==HARDWARE_TYPE.DMG1)
				VRAMBANK0[addr-0x8000] = val;
			else {
				if ((RAM[ADDR_0xFF4F_VRAMBANK]&1)==0) {
					VRAMBANK0[addr-0x8000] = val;
				}
				else
				{
					VRAMBANK1[addr-0x8000] = val;
				}
			}
		}


		// Writing 1 to this address switches the bios out.
		if (addr == 0xFF50 && val == 1) {
			// RAM[0xFF50]=1;
			biosActive = false;
			//writeBigMessage("Switched bios!!!", 10);
		}

		if (addr == 0x0151) {
			//writeBigMessage("wrote to 0x0150!!!", 1000);
		}

		if (addr == ADDR_FF46_DMA_TRANSFER) {
			// RAM[addr] = val;
			transferDMA(val);
			return;
		}

		// Writing anything to the scanline register resets it.
		if (addr == ADDR_FF44_Y_SCANLINE) {
			RAM[addr] = 0;
			return;
		}

		// GBC specific background palette
		if (addr == ADDR_0xFF69_BGPALETTEDATA) {
			boolean auto = (RAM[ADDR_0xFF68_BGPALETTEINDEX]&0x80)>0?true:false;
			int pIndex = RAM[ADDR_0xFF68_BGPALETTEINDEX]&0x3F;
			cpu.gpu.cgbBackgroundPaletteData[pIndex]=val;
			if (auto) {
				pIndex = (pIndex+1)&0x3F;
				RAM[ADDR_0xFF68_BGPALETTEINDEX]&=0xc0;
				RAM[ADDR_0xFF68_BGPALETTEINDEX]|=pIndex;
			}
		}
		
		// GBC specific sprite palette
		if (addr == ADDR_0xFF6B_SPRITEPALETTEDATA) {
			boolean auto = (RAM[ADDR_0xFF6A_SPRITEPALETTEINDEX]&0x80)>0?true:false;
			int pIndex = RAM[ADDR_0xFF6A_SPRITEPALETTEINDEX]&0x3F;
			cpu.gpu.cgbSpritePaletteData[pIndex]=val;
			if (auto) {
				pIndex = (pIndex+1)&0x3F;
				RAM[ADDR_0xFF6A_SPRITEPALETTEINDEX]&=0xc0;
				RAM[ADDR_0xFF6A_SPRITEPALETTEINDEX]|=pIndex;
			}
		}

		// this area is restricted
		if ((addr >= 0xFEA0) && (addr < 0xFEFF)) {
		}

		// RAM BANK 1 (Switchable on CGB)
		// * D000 DFFF 4KB Work RAM (WRAM) bank 1~N Only bank 1 in Non-CGB mode /
		// Switchable bank 1~7 in CGB mode
		if (inRange(addr, 0xD000, 0xDFFF)) {
			if (cpu.hardwareType==HARDWARE_TYPE.DMG1)
				RAM[addr]=val;
			else {
				// Ram switch
				int bank1Switch = RAM[0xFF70]&0x07;
				if (bank1Switch==0) bank1Switch=1;
				int normalisedAddress = addr-0xd000;
				RAM_BANKS1[(bank1Switch*0x1000)+addr]=val;
			}
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
			//System.out.println("Peeked 0xff00 at "+cpu.tickCount);
			//return 0xff;
			return cpu.input.peekFF00();
		}

		// TODO Serial registers
		// some games need this to be able to play (EG alleyway)
		if (addr == 0xFF01) {
			return 0xFF;
		}
		if (addr == 0xFF02) {
			return 0xff;
		}
		
		// temp hack test 
		// this memory address is causing oracle of ages to halt
//		if (addr == 0xFF4d) {
//			return cpu.tickCount&0xff;
//		}
		
		// Handle special negative addresses (registers)
		if (addr < 0) {
			return peekSpecial(addr);
		}

		if (isSwitchableddress(addr)) {
			return memoryBank.peek(addr);
		}
		
		// VRAM
		if (addr>=0x8000 && addr<0x9FFF) {
			if (cpu.hardwareType==HARDWARE_TYPE.DMG1)
				return VRAMBANK0[addr-0x8000];
			else {
				if ((RAM[ADDR_0xFF4F_VRAMBANK]&1)==0) {
					return VRAMBANK0[addr-0x8000];
				}
				else
				{
					return VRAMBANK1[addr-0x8000];
				}
			}
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


		// GBC palette
		// ADDR_0xFF68_BGPALETTEINDEX
		if (addr == ADDR_0xFF69_BGPALETTEDATA) {
			int pIndex = RAM[ADDR_0xFF68_BGPALETTEINDEX]&0x3F;
			return cpu.gpu.cgbBackgroundPaletteData[pIndex];
		}
		
		if (addr == ADDR_0xFF6B_SPRITEPALETTEDATA) {
			int pIndex = RAM[ADDR_0xFF6A_SPRITEPALETTEINDEX]&0x3F;
			return cpu.gpu.cgbSpritePaletteData[pIndex];
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


		// RAM BANK 0
		// * C000 CFFF 4KB Work RAM (WRAM) bank 0
		if (inRange(addr, 0xC000, 0xCFFF)) {
			return RAM[addr];
		}

		// RAM BANK 1 (Switchable on CGB)
		// * D000 DFFF 4KB Work RAM (WRAM) bank 1~N Only bank 1 in Non-CGB mode /
		// Switchable bank 1~7 in CGB mode
		if (inRange(addr, 0xD000, 0xDFFF)) {
			if (cpu.hardwareType==HARDWARE_TYPE.DMG1)
				return RAM[addr];
			else {
				// Ram switch
				int bank1Switch = RAM[0xFF70]&0x07;
				if (bank1Switch==0) bank1Switch=1;
				int normalisedAddress = addr-0xd000;
				return RAM_BANKS1[(bank1Switch*0x1000)+addr];
			}
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
