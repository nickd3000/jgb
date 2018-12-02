package com.physmo.jgb;

import java.awt.Color;

import com.physmo.toolbox.BasicDisplay;

/*
	8000-87FF	Tile set #1: tiles 0-127
	8800-8FFF	Tile set #1: tiles 128-255
				Tile set #0: tiles -1 to -128
	9000-97FF	Tile set #0: tiles 0-127
	9800-9BFF	Tile map #0
	9C00-9FFF	Tile map #1
	
	0xFF40 - display control
	 Bit 7 - LCD Display Enable             (0=Off, 1=On)
	 Bit 6 - Window Tile Map Display Select (0=9800-9BFF, 1=9C00-9FFF)
	 Bit 5 - Window Display Enable          (0=Off, 1=On)
	 Bit 4 - BG & Window Tile Data Select   (0=8800-97FF, 1=8000-8FFF)
	 Bit 3 - BG Tile Map Display Select     (0=9800-9BFF, 1=9C00-9FFF)
	 Bit 2 - OBJ (Sprite) Size              (0=8x8, 1=8x16)
	 Bit 1 - OBJ (Sprite) Display Enable    (0=Off, 1=On)
	 Bit 0 - BG/Window Display/Priority     (0=Off, 1=On)
	 
	 0xFF47 background palette
	 0xFF48 spr palette 1 
	 0xFF49 spr palette 2
 */
public class GPU {
	public  final int scale = 2;
	public  int clock = 0;
	public  int currentMode = 0;

	public static final int ADDR_0xFF47_BGPALETTE = 0xFF47;
	
	public static final Color c1 = new Color(0, 0, 0);
	public static final Color c2 = new Color(70, 70, 70);
	public static final Color c3 = new Color(180, 180, 180);
	public static final Color c4 = new Color(255, 255, 255);
	public static final Color sc1 = new Color(30, 0, 0);
	public static final Color sc2 = new Color(80, 70, 70);
	public static final Color sc3 = new Color(200, 180, 180);
	public static final Color sc4 = new Color(255, 235, 225);
	
	// public static int scanline = 0;
	public  int lastLineRendered = 0;
	
	public  Sprite [] sprites = new Sprite[40];
	
	public GPU() {
		for (int i=0;i<40;i++) {
			sprites[i]=new Sprite();
		}
	}

	Color backgroundPaletteMaster [] = {
			new Color(0, 0, 0),
			new Color(70, 70, 70),
			new Color(180, 180, 180),
			new Color(255, 255, 255)
	};
	Color sprite1PaletteMaster [] = {
			new Color(20, 0, 0),
			new Color(80, 70, 70),
			new Color(200, 180, 180),
			new Color(255, 245, 245)
	};
	Color sprite2PaletteMaster [] = {
			new Color(0, 20, 0),
			new Color(70, 80, 70),
			new Color(180, 200, 180),
			new Color(245, 255, 245)
	};
	Color backgroundPaletteMap [] = new Color[4];
	Color sprite1PaletteMap [] = new Color[4];
	Color sprite2PaletteMap [] = new Color[4];
	
	public void processPalettes(CPU cpu) {
		int bgPalette = cpu.mem.RAM[ADDR_0xFF47_BGPALETTE];
		backgroundPaletteMap[3] = backgroundPaletteMaster[(bgPalette&3)];
		backgroundPaletteMap[2] = backgroundPaletteMaster[((bgPalette>>2)&3)];
		backgroundPaletteMap[1] = backgroundPaletteMaster[((bgPalette>>4)&3)];
		backgroundPaletteMap[0] = backgroundPaletteMaster[((bgPalette>>6)&3)];
		
		int sprPalette1 = cpu.mem.RAM[0xFF48];
		sprite1PaletteMap[3] = sprite1PaletteMaster[(sprPalette1&3)];
		sprite1PaletteMap[2] = sprite1PaletteMaster[((sprPalette1>>2)&3)];
		sprite1PaletteMap[1] = sprite1PaletteMaster[((sprPalette1>>4)&3)];
		sprite1PaletteMap[0] = sprite1PaletteMaster[((sprPalette1>>6)&3)];
		
		int sprPalette2 = cpu.mem.RAM[0xFF49];
		sprite2PaletteMap[3] = sprite2PaletteMaster[(sprPalette2&3)];
		sprite2PaletteMap[2] = sprite2PaletteMaster[((sprPalette2>>2)&3)];
		sprite2PaletteMap[1] = sprite2PaletteMaster[((sprPalette2>>4)&3)];
		sprite2PaletteMap[0] = sprite2PaletteMaster[((sprPalette2>>6)&3)];
	}
	
	public  void setLCDRegisterMode(CPU cpu, int mode) {
		// 0: During H-Blank
		// 1: During V-Blank
		// 2: During Searching OAM-RAM
		// 3: During Transfering Data to LCD Driver
		int val = cpu.mem.RAM[0xFF41];
		val &= 0b1111_1100;
		val += (mode & 3);
		cpu.mem.RAM[0xFF41] = val;
	}

	public void debug(CPU cpu, BasicDisplay bd) {
		bd.setDrawColor(Color.GREEN);
		bd.drawText("FL: "+Utils.toHex2(cpu.FL), 350-100, 50);
	}
	
	public void tick(CPU cpu, BasicDisplay bd, int cycles) {

		//debug(cpu, bd);
		
		clock = (clock + cycles) & 0xFFFFFFFF;

		processPalettes(cpu);
		
		
		int y = cpu.mem.RAM[0xFF44]; // Scanline register
		int lcdStat = cpu.mem.RAM[CPU.ADDR_FF41_LCD_STAT];
		//int y = cpu.mem.peek(0xFF44); // Scanline register
		//int lcdStat = cpu.mem.peek(CPU.ADDR_FF41_LCD_STAT);
		
		if (y < 144) {
			renderLine(cpu, bd, y);
		}

		y = y + 1;

		if (y == 144) {
			cpu.mem.RAM[0xFF0F] |= CPU.INT_VBLANK;
		}

		int previousMode = currentMode;
		boolean statChanged = false;

		if (y >= 144) {
			currentMode = 1;
			if ((lcdStat & 0x10) > 0)
				statChanged = true;
		} else {
			if (clock <= 80) {
				// Mode 2
				currentMode = 2;
				if ((lcdStat & 0x20) > 0)
					statChanged = true;

			} else if (clock >= 80 && clock < 252) {
				// Mode 3
				currentMode=3;
				//setLCDRegisterMode(cpu, 3);
			} else if (clock >= 252 && clock < 456) {
				// Mode 0
				currentMode = 0;
				if ((lcdStat & 0x08) > 0)
					statChanged = true;

			}
		}

		setLCDRegisterMode(cpu, currentMode);
		if (currentMode!=previousMode && statChanged) {
			cpu.requestInterrupt(CPU.INT_LCDSTAT);
		}


		if (clock >= 456) {
			if (y < 144 && lastLineRendered != y) {
				lastLineRendered = y;
				renderLine(cpu, bd, y);
			}

			clock = 0;
			y++;

			if (y == 144) {
				// this.system.requestInterrupt(0);
				cpu.requestInterrupt(CPU.INT_VBLANK);
				cpu.requestInterrupt(CPU.INT_LCDSTAT);
			} else if (y > 153 + 10) {
				y = 0;
				// this.drawScreen();
				bd.refresh();
			}
		}

		
		   // check the conincidence flag
//		   if (ly == ReadMemory(0xFF45))
//		   {
//		     status = BitSet(status,2) ;
//		     if (TestBit(status,6))
//		       RequestInterupt(1) ;
//		   }
//		   else
//		   {
//		     status = BitReset(status,2) ;
//		   }
//		   WriteMemory(0xFF41,status) ; 
//		   

		// Handle y coincidence check.
		//int ycompare = cpu.mem.peek(CPU.ADDR_FF45_Y_COMPARE);
		int ycompare = cpu.mem.RAM[CPU.ADDR_FF45_Y_COMPARE];
		if (y == ycompare) {
			cpu.mem.RAM[CPU.ADDR_FF41_LCD_STAT] |= 0x04;
			if ((cpu.mem.RAM[CPU.ADDR_FF41_LCD_STAT] & (1<<6))>0) {
				cpu.requestInterrupt(CPU.INT_LCDSTAT);
			}
		} else {
			cpu.mem.RAM[CPU.ADDR_FF41_LCD_STAT] &= ~0x04;
		}
		
		
		cpu.mem.RAM[0xFF44] = y;
		//cpu.mem.poke(0xFF44,y);
	}

	static int tileDataPtr = 0x8800;

//	public  int getTilePixel(CPU cpu, int tileId, int subx, int suby) {
//		int byteOffset = (suby * 2) + (subx / 4);
//		int data = cpu.mem.RAM[tileDataPtr + byteOffset];
//		int nibble = (subx / 2) & 3;
//		switch (nibble) {
//		case 0:
//			return data & 3;
//		case 1:
//			return (data >> 2) & 3;
//		case 2:
//			return (data >> 4) & 3;
//		case 3:
//			return (data >> 6) & 3;
//		}
//
//		return 0;
//	}

	static int spriteDataPtr = 0x8000;
	public  int getSpritePixel2(CPU cpu, int tileId, int subx, int suby) {
		int byteOffset = (suby * 2);

		int data1 = cpu.mem.RAM[spriteDataPtr + (tileId * 16) + byteOffset];
		int data2 = cpu.mem.RAM[spriteDataPtr + (tileId * 16) + byteOffset + 1];
		int mask = 1 << (7 - subx);
		int color = 0;
		if ((data1 & mask) > 0)
			color += 0b0001;
		if ((data2 & mask) > 0)
			color += 0b0010;

		return color;
	}
	
	public  int getTilePixel2(CPU cpu, int tileId, int subx, int suby) {
		int byteOffset = (suby * 2);
		;

		int data1 = cpu.mem.RAM[tileDataPtr + (tileId * 16) + byteOffset];
		int data2 = cpu.mem.RAM[tileDataPtr + (tileId * 16) + byteOffset + 1];
		int mask = 1 << (7 - subx);
		int color = 0;
		if ((data1 & mask) > 0)
			color += 0b0001;
		if ((data2 & mask) > 0)
			color += 0b0010;

		return color;
	}

	public  void renderLine(CPU cpu, BasicDisplay bd, int y) {

		int vram = 0x8010;
		
		getSprites(cpu);
		
		boolean signedTileIndices = false;
		int lcdControl = cpu.mem.RAM[0xFF40];

		// Bit 3 - BG Tile Map Display Select (0=9800-9BFF, 1=9C00-9FFF)
		if ((lcdControl & (1 << 3)) == 0)
			vram = 0x9800;
		else
			vram = 0x9C00;

		// Set tile data pointer.
		// Bit 4 - BG & Window Tile Data Select (0=8800-97FF, 1=8000-8FFF)
		if ((lcdControl & (1 << 4)) == 0) {
			tileDataPtr = 0x8800;
			signedTileIndices = true;
		} else {
			tileDataPtr = 0x8000;
			signedTileIndices = false;
		}

		// if(GPU._bgtile == 1 && tile < 128) tile += 256;
		// int tileNum = signedTileNumbers ? ((int)bTileNum + 128) : (bTileNum & 0xff);

		int scrolly = cpu.mem.RAM[0xFF42];
		int scrollx = cpu.mem.RAM[0xFF43];
		int xx, yy;

		yy = y + scrolly;
		int subx = 0, suby = yy % 8;

		for (int x = 0; x < 160; x++) {
			xx = x + scrollx;
			
			// Wrap xx and yy
			xx=xx&(0xff);
			yy=yy&(0xff);
			
			int charOffset = (xx / 8) + ((yy / 8) * 32);
			int charIndex = (byte) cpu.mem.RAM[vram + charOffset];

			if (signedTileIndices)
				charIndex += 128;
			else
				charIndex = charIndex & 0xff;

			subx = xx % 8;

			int tp = getTilePixel2(cpu, charIndex, subx, suby);

			int pix = tp; //+charIndex;

			// Set draw colour from pixel data.
			bd.setDrawColor(backgroundPaletteMap[pix&3]);
			

			// Sprites
			// Bit6   Y flip          (0=Normal, 1=Vertically mirrored)
			// Bit5   X flip          (0=Normal, 1=Horizontally mirrored)
			int sprPalette = 0;
			for (int i=0;i<40;i++) {
				if (x>=sprites[i].x && x<sprites[i].x+8) {
					if (y>=sprites[i].y && y<sprites[i].y+8) {
						//bd.setDrawColor(Color.GREEN);
						int sprsubx = (x-sprites[i].x)&7;
						int sprsuby = (y-sprites[i].y)&7;
						if (((sprites[i].attributes)&(1<<5))>0) sprsubx=7-sprsubx;
						if (((sprites[i].attributes)&(1<<6))>0) sprsuby=7-sprsuby;
						if (((sprites[i].attributes)&(1<<4))>0) sprPalette=1; // Bit4: Palette number
						
						int sprPixel = getSpritePixel2(cpu, sprites[i].tileId, sprsubx, sprsuby);
						
						if (sprPixel>0)
							bd.setDrawColor(getSpriteCol(sprPixel, sprPalette));
					}
				}
			}
			
			bd.drawFilledRect(x * scale, y * scale, scale, scale);
		}
	}
	
	public Color getSpriteCol(int i, int pal) {
		
		if (pal==0) {
			return sprite1PaletteMap[i&3];
		} else {
			return sprite2PaletteMap[i&3];
		}
		
		/*
		switch(i&3) {
		case 0: return sc1;
		case 1: return sc2;
		case 2: return sc3;
		case 3: return sc4;
		}
		return null;
		*/
	}
	
	public  void getSprites(CPU cpu) {
		for (int i=0;i<40;i++) {
			getSprite(cpu,i);
		}
	}
	
	public  void getSprite(CPU cpu, int id) {
        int spriteAddress = 0xFE00 + (id * 4);
        sprites[id].y = cpu.mem.RAM[spriteAddress] - 16; // Offset for display window.
        sprites[id].x = cpu.mem.RAM[spriteAddress+1] - 8; // Offset for display window.
        sprites[id].tileId = cpu.mem.RAM[spriteAddress+2];
        sprites[id].attributes = cpu.mem.RAM[spriteAddress+3];
	}
        
}

class Sprite {
	int x,y,tileId,attributes;
}

