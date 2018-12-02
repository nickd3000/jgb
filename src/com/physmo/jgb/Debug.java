package com.physmo.jgb;

public class Debug {

	public static String dissasemble(CPU cpu, int addr) {

		String str = "> ";
		int instr = cpu.mem.peek(addr);
		InstructionDefinition def = InstructionDefinition.getEnumFromId(instr);

		String strBytes = "";
		int numBytes = def.getNumBytes();
		for (int i = 0; i < numBytes; i++) {
			strBytes += " " + Utils.toHex2(cpu.mem.peek(addr + i));
		}
		strBytes = Utils.padToLength(strBytes, 10);

		str += "0x" + Utils.toHex4(addr); // Address
		str += "  " + strBytes;
		str += "  " + def.toString();

		str = Utils.padToLength(str, 40 - 6);

		return str;
	}

	public static String getFlags(CPU cpu) {
		String str = "";

		str += cpu.testFlag(CPU.FLAG_ZERO) ? "zf " : " - ";
		str += cpu.testFlag(CPU.FLAG_ADDSUB) ? " n" : " - ";
		str += cpu.testFlag(CPU.FLAG_HALFCARRY) ? " hc " : "  - ";
		str += cpu.testFlag(CPU.FLAG_CARRY) ? " c " : " - ";

		return str;
	}

	/*
	 * int A,B,C,D,E,H,L; int PC; // Program counter int SP; // Stack pointer int
	 * FL;
	 */
	public static String getRegisters(CPU cpu) {
		String str = "";

		str += "A:" + Utils.toHex2(cpu.A);
		str += " B:" + Utils.toHex2(cpu.B);
		str += " C:" + Utils.toHex2(cpu.C);
		str += " D:" + Utils.toHex2(cpu.D);
		str += " E:" + Utils.toHex2(cpu.E);
		str += " H:" + Utils.toHex2(cpu.H);
		str += " L:" + Utils.toHex2(cpu.L);
		str += " PC:" + Utils.toHex4(cpu.PC);
		str += " SP:" + Utils.toHex4(cpu.SP);
		str += " FL:" + Utils.toHex2(cpu.FL);

		return str;
	}

	public static String getStackData(CPU cpu, int numItems) {
		// int top = 0xfffe;
		int top = cpu.topOfSTack;
		int stackSize = ((top - cpu.SP) / 2);
		String str = "  STK:(" + stackSize + ")";
		int ptr = cpu.SP;
		if (ptr < 10)
			return "invalid stack";
		if (numItems > stackSize)
			numItems = stackSize;
		int b1, b2;
		for (int i = 0; i < numItems; i++) {
			// if (ptr<=0xff-1)
			b1 = cpu.mem.RAM[++ptr];
			b2 = cpu.mem.RAM[++ptr];
			str += " " + Utils.toHex4(cpu.combineBytes(b2, b1));
		}
		return str;
	}

	public static String getInterruptFlags(CPU cpu) {
		int if1 = cpu.mem.RAM[0xFFFF];
		int if2 = cpu.mem.RAM[0xFF0F];

		String str = "INT:" + if1 + "|" + if2 + " ie:" + cpu.interruptEnabled;
		return str;
	}

	// Print section of memory as if it is being peeked.
	public static void printMem(CPU cpu, int start, int length) {
		String line = "";
		int end = start + length;
		int addr = start;

		while (addr < end)
			line += " 0x" + Utils.toHex2(cpu.mem.peek(addr++));

		System.out.println(line);
	}

	public static void checkInstructionDefs() {

		for (InstructionDefinition id : InstructionDefinition.values()) {
			ADDRMODE am1 = id.getAddressMode1();
			ADDRMODE am2 = id.getAddressMode2();

			int total = getAddressModeBytes(am1) + getAddressModeBytes(am2);
			int numBytes = id.getNumBytes();
		}

	}

	public static int getAddressModeBytes(ADDRMODE a) {
		switch (a) {

		case __nnnn:
			return 2;
		case nnnn:
			return 2;
		case nn:
			return 1;

		default:
			return 0;
		}

	}

	public static void checkRegisters(CPU cpu) {
		checkRegister8Bit(cpu, cpu.A, "A");
		checkRegister8Bit(cpu, cpu.B, "B");
		checkRegister8Bit(cpu, cpu.C, "C");
		checkRegister8Bit(cpu, cpu.D, "D");
		checkRegister8Bit(cpu, cpu.E, "E");
		checkRegister8Bit(cpu, cpu.H, "H");
		checkRegister8Bit(cpu, cpu.L, "L");
	}

	public static void checkRegister8Bit(CPU cpu, int val, String name) {
		if (val < 0 || val > 0xff) {
			System.out.println("Register overflow: " + name + "=" + val + "  PC=" + Utils.toHex4(cpu.PC)+ "  tick:"+cpu.tickCount);
			cpu.mem.poke(0xffff + 10, 1);
		}
	}

	public static String getCartridgeType(CPU cpu) {
		int type = cpu.mem.peek(0x0147);

		// Specifies which Memory Bank Controller (if any) is used in the cartridge, and
		// if further external hardware exists in the cartridge.
		switch (type) {
		case 0x00:
			return "ROM ONLY";
		case 0x01:
			return "MBC1";
		case 0x02:
			return "MBC1+RAM";
		case 0x03:
			return "MBC1+RAM+BATTERY";
		case 0x05:
			return "MBC2";
		case 0x06:
			return "MBC2+BATTERY";
		default:
			return "Unrecognised: 0x" + Utils.toHex2(type);
		}

		/*
		 * 00h ROM ONLY 13h MBC3+RAM+BATTERY 01h MBC1 15h MBC4 02h MBC1+RAM 16h MBC4+RAM
		 * 03h MBC1+RAM+BATTERY 17h MBC4+RAM+BATTERY 05h MBC2 19h MBC5 06h MBC2+BATTERY
		 * 1Ah MBC5+RAM 08h ROM+RAM 1Bh MBC5+RAM+BATTERY 09h ROM+RAM+BATTERY 1Ch
		 * MBC5+RUMBLE 0Bh MMM01 1Dh MBC5+RUMBLE+RAM 0Ch MMM01+RAM 1Eh
		 * MBC5+RUMBLE+RAM+BATTERY 0Dh MMM01+RAM+BATTERY FCh POCKET CAMERA 0Fh
		 * MBC3+TIMER+BATTERY FDh BANDAI TAMA5 10h MBC3+TIMER+RAM+BATTERY FEh HuC3 11h
		 * MBC3 FFh HuC1+RAM+BATTERY 12h MBC3+RAM
		 */
	}

}
