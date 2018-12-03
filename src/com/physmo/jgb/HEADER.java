package com.physmo.jgb;

public class HEADER {

	// TODO: rewrite this to use enums instead of ints.
	enum MBCTYPE {ROM_ONLY, MBC1}

	public static int getMemoryBankControllerType(CPU cpu) {
		int type = cpu.mem.peek(0x0147);
		return type;
	}
	
	public static String getMemoryBankControllerName(CPU cpu) {
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
