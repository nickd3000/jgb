package com.physmo.jgb;

import java.awt.*;

public class PaletteGenerator {

	enum PALETTE_TYPE {
		CLASSIC, SUPER_GAMEBOY, NEWISH
	}

	public static Color get(PALETTE_TYPE type, int index) {

		switch (type) {
		case CLASSIC:
			if (index == 3)
				return new Color(0x24, 0x31, 0x37);
			if (index == 2)
				return new Color(0x3f, 0x50, 0x3f);
			if (index == 1)
				return new Color(0x76, 0x84, 0x48);
			if (index == 0)
				return new Color(0xac, 0xb5, 0x6b);
			break;
		case SUPER_GAMEBOY:
			if (index == 3)
				return new Color(0x33, 0x1e, 0x50);
			if (index == 2)
				return new Color(0xa6, 0x37, 0x25);
			if (index == 1)
				return new Color(0xd6, 0x8e, 0x49);
			if (index == 0)
				return new Color(0xf7, 0xe7, 0xc6);
			break;
		case NEWISH:
			if (index == 0)
				return new Color( 0x1e, 0x50,0x33);
			if (index == 1)
				return new Color( 0x37, 0x25, 0xa6);
			if (index == 2)
				return new Color( 0x8e, 0x49, 0xd6);
			if (index == 3)
				return new Color(0xe7, 0xc6, 0xf7);
			break;
			
		}

		return null;
	}
}