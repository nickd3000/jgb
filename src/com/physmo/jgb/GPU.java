package com.physmo.jgb;

import java.awt.Color;

import com.physmo.jgb.PaletteGenerator.PALETTE_TYPE;
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
	public int scale = 3;
	public int clock = 0;
	public int currentMode = 0;
	static int tileDataPtr = 0x8800;
	static int spriteDataPtr = 0x8000;

	
	// public static int scanline = 0;
	public  int lastLineRendered = 0;
	
	public  Sprite [] sprites = new Sprite[40];
	public int [] cgbBackgroundPaletteData = new int[64];
	public int [] cgbSpritePaletteData = new int[64];
	
	public GPU(int scale) {
		this.scale = scale;
		for (int i=0;i<40;i++) {
			sprites[i]=new Sprite();
		}
//		for (int i=0;i<64;i++) {
//			cgbBackgroundPaletteData[i]=(i*2)&0xff;;
//			cgbSpritePaletteData[i]=(i*2)&0xff;;
//		}
	}

	PALETTE_TYPE paletteType = PALETTE_TYPE.CLASSIC;
	Color backgroundPaletteMaster [] = {
			PaletteGenerator.get(paletteType, 0),
			PaletteGenerator.get(paletteType, 1),
			PaletteGenerator.get(paletteType, 2),
			PaletteGenerator.get(paletteType, 3)
	};
	Color sprite1PaletteMaster [] = {
			PaletteGenerator.get(paletteType, 0),
			PaletteGenerator.get(paletteType, 1),
			PaletteGenerator.get(paletteType, 2),
			PaletteGenerator.get(paletteType, 3)
	};
	Color sprite2PaletteMaster [] = {
			PaletteGenerator.get(paletteType, 0),
			PaletteGenerator.get(paletteType, 1),
			PaletteGenerator.get(paletteType, 2),
			PaletteGenerator.get(paletteType, 3)
	};
	Color backgroundPaletteMap [] = new Color[4];
	Color sprite1PaletteMap [] = new Color[4];
	Color sprite2PaletteMap [] = new Color[4];
	
	public void processPalettes(CPU cpu) {
		//int bgPalette = cpu.mem.RAM[MEM.ADDR_0xFF47_BGPALETTE];
		int bgPalette = cpu.mem.peek(MEM.ADDR_0xFF47_BGPALETTE);
		
		backgroundPaletteMap[0] = backgroundPaletteMaster[(bgPalette&0b11)];
		backgroundPaletteMap[1] = backgroundPaletteMaster[((bgPalette>>2)&0b11)];
		backgroundPaletteMap[2] = backgroundPaletteMaster[((bgPalette>>4)&0b11)];
		backgroundPaletteMap[3] = backgroundPaletteMaster[((bgPalette>>6)&0b11)];
		
		//int sprPalette1 = cpu.mem.RAM[MEM.ADDR_0xFF48_SPRITEPALETTE1];
		int sprPalette1 = cpu.mem.peek(MEM.ADDR_0xFF48_SPRITEPALETTE1);
		sprite1PaletteMap[0] = sprite1PaletteMaster[(sprPalette1&3)];
		sprite1PaletteMap[1] = sprite1PaletteMaster[((sprPalette1>>2)&3)];
		sprite1PaletteMap[2] = sprite1PaletteMaster[((sprPalette1>>4)&3)];
		sprite1PaletteMap[3] = sprite1PaletteMaster[((sprPalette1>>6)&3)];
		
		//int sprPalette2 = cpu.mem.RAM[MEM.ADDR_0xFF49_SPRITEPALETTE2];
		int sprPalette2 = cpu.mem.peek(MEM.ADDR_0xFF49_SPRITEPALETTE2);
		sprite2PaletteMap[0] = sprite2PaletteMaster[(sprPalette2&3)];
		sprite2PaletteMap[1] = sprite2PaletteMaster[((sprPalette2>>2)&3)];
		sprite2PaletteMap[2] = sprite2PaletteMaster[((sprPalette2>>4)&3)];
		sprite2PaletteMap[3] = sprite2PaletteMaster[((sprPalette2>>6)&3)];
	}
	
	public Color [] cgbBackgroundColors = new Color[64];
	public Color [] cgbSpriteColors = new Color[64];
	
	public void processGBCPalettes(CPU cpu) {
		int col=0;
		for (int i=0;i<64;i+=2) {
			cgbBackgroundColors[col] = createColorFrom2ByteValue(
					cgbBackgroundPaletteData[i],
					cgbBackgroundPaletteData[i+1]);
			
			cgbSpriteColors[col] = createColorFrom2ByteValue(
					cgbSpritePaletteData[i],
					cgbSpritePaletteData[i+1]);
			col++;
		}
	}
	
	
	// Bit 0-4   Red Intensity   (00-1F)
	// Bit 5-9   Green Intensity (00-1F)
	// Bit 10-14 Blue Intensity  (00-1F)
	public Color createColorFrom2ByteValue(int b2, int b1) {
		int scale=8;
		int combined = ((b1&0xff)<<8)|(b2&0xff);
		int r = (combined)&0x1f;
		int g = (combined>>5)&0x1f;
		int b = (combined>>10)&0x1f;
		return new Color(r*scale,g*scale,b*scale);
	}
	
	public  void setLCDRegisterMode(CPU cpu, int mode) {
		// 0: During H-Blank
		// 1: During V-Blank
		// 2: During Searching OAM-RAM
		// 3: During Transfering Data to LCD Driver
		//int val = cpu.mem.RAM[MEM.ADDR_FF41_LCD_STAT];
		int val = cpu.mem.peek(MEM.ADDR_FF41_LCD_STAT);
		val &= 0b1111_1100;
		val += (mode & 3);
		//cpu.mem.RAM[MEM.ADDR_FF41_LCD_STAT] = val;
		cpu.mem.poke(MEM.ADDR_FF41_LCD_STAT, val);
	}

	public void debug(CPU cpu, BasicDisplay bd) {
		bd.setDrawColor(Color.GREEN);
		bd.drawText("FL: "+Utils.toHex2(cpu.FL), 350-100, 50);
	}
	
	public void tick(CPU cpu, BasicDisplay bd, int cycles) {

		//debug(cpu, bd);
		
		
		int previousMode = currentMode;
		int y = cpu.mem.RAM[MEM.ADDR_0xFF44_SCANLINE]; // Scanline register
		int lcdStat = cpu.mem.RAM[MEM.ADDR_FF41_LCD_STAT];
		int lcdControl = cpu.mem.RAM[0xFF40];

		boolean lcdEnabled = testBit(lcdControl,7);
		
		// Handle LCD OFF
		if (!lcdEnabled) {
			cpu.mem.RAM[MEM.ADDR_0xFF44_SCANLINE]=0;
			cpu.mem.RAM[0xFF41]&=0b11111100;
			
			if (previousMode!=0 && testBit(lcdStat,3)) {
				cpu.requestInterrupt(CPU.INT_LCDSTAT);
			}
			
			setLCDRegisterMode(cpu, 0);
			clock=0;
			return;
		}
		
		clock = (clock + cycles) & 0xFFFFFFFF;

		boolean modeInterruptEnabled = false;

		if (y >= 144) {
			currentMode = 1;
			
			if (testBit(lcdStat,4))
				modeInterruptEnabled = true;
			 
		} else {
			if (clock <= 80) {
				// Mode 2
				currentMode = 2;
				
				if (testBit(lcdStat,5))
					modeInterruptEnabled = true;

			} else if (clock >= 80 && clock < 252) {
				// Mode 3
				currentMode=3;
				//setLCDRegisterMode(cpu, 3);
			} else if (clock >= 252 && clock < 456) {
				// Mode 0
				currentMode = 0;
				
				if (testBit(lcdStat,3))
					modeInterruptEnabled = true;

			}
		}

		setLCDRegisterMode(cpu, currentMode);
		if (currentMode!=previousMode && modeInterruptEnabled) {
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
				//cpu.requestInterrupt(CPU.INT_LCDSTAT);
			} else if (y > 153 + 10) {
				y = 0;
				// this.drawScreen();
				bd.refresh();
//				try {
//					Thread.sleep(5);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
		}

		
		// Handle y coincidence check.
		//int ycompare = cpu.mem.peek(CPU.ADDR_FF45_Y_COMPARE);
		int ycompare = cpu.mem.RAM[MEM.ADDR_FF45_Y_COMPARE];
		if (y == ycompare) {
			cpu.mem.RAM[MEM.ADDR_FF41_LCD_STAT] |= 0x04;
			if ((cpu.mem.RAM[MEM.ADDR_FF41_LCD_STAT] & (1<<6))>0) {
				cpu.requestInterrupt(CPU.INT_LCDSTAT);
			}
		} else {
			cpu.mem.RAM[MEM.ADDR_FF41_LCD_STAT] &= ~0x04;
		}
		
		
		cpu.mem.RAM[MEM.ADDR_FF44_Y_SCANLINE] = y;
		//cpu.mem.poke(0xFF44,y);
	}

	
	public  int getSpritePixel2(CPU cpu, int tileId, int subx, int suby) {
		int byteOffset = (suby * 2);

		//int data1 = cpu.mem.RAM[spriteDataPtr + (tileId * 16) + byteOffset];
		//int data2 = cpu.mem.RAM[spriteDataPtr + (tileId * 16) + byteOffset + 1];
		int data1 = cpu.mem.peek(spriteDataPtr + (tileId * 16) + byteOffset);
		int data2 = cpu.mem.peek(spriteDataPtr + (tileId * 16) + byteOffset + 1);
				
		int mask = 1 << (7 - subx);
		int color = 0;
		if ((data1 & mask) > 0)
			color += 0b0001;
		if ((data2 & mask) > 0)
			color += 0b0010;

		return color;
	}
	
	public  int getTilePixel(CPU cpu, int tileId, int subx, int suby, int bank) {
		int byteOffset = (suby * 2);

		int normalisedPointer = tileDataPtr-0x8000;
		
		//int data1 = cpu.mem.RAM[tileDataPtr + (tileId * 16) + byteOffset];
		//int data2 = cpu.mem.RAM[tileDataPtr + (tileId * 16) + byteOffset + 1];
		//int data1 = cpu.mem.peek(tileDataPtr + (tileId * 16) + byteOffset);
		//int data2 = cpu.mem.peek(tileDataPtr + (tileId * 16) + byteOffset + 1);
		int data1 = cpu.mem.VRAMBANK0[normalisedPointer + (tileId * 16) + byteOffset];
		int data2 = cpu.mem.VRAMBANK0[normalisedPointer + (tileId * 16) + byteOffset + 1];
		
		if (bank==1) {
			data1=cpu.mem.VRAMBANK1[normalisedPointer + (tileId * 16) + byteOffset];
			data2=cpu.mem.VRAMBANK1[normalisedPointer + (tileId * 16) + byteOffset + 1];
		}
		
		
		int mask = 1 << (7 - subx);
		int color = 0;
		if ((data1 & mask) > 0)
			color |= 0b01;
		if ((data2 & mask) > 0)
			color |= 0b10;

		return color;
	}

	public boolean testBit(int value, int bit) {
		if ((value&(1<<bit))>0) return true;
		return false;
	}
	
	public  void renderLine(CPU cpu, BasicDisplay bd, int y) {

		int bgTileMapLocation = 0x8010;
		
		processPalettes(cpu);
		processGBCPalettes(cpu);
		getSprites(cpu);
		
		boolean signedTileIndices = false;
		int lcdControl = cpu.mem.RAM[0xFF40];
		int spriteHeight=8;
		
		// Bit 1 - OBJ (Sprite) Display Enable    (0=Off, 1=On)
		boolean spritesEnabled = testBit(lcdControl,1);
		
		// Bit 5 - Window Display Enable          (0=Off, 1=On)
		boolean windowEnabled = testBit(lcdControl,5);
		

		// Bit 3 - BG Tile Map Display Select (0=9800-9BFF, 1=9C00-9FFF)
		if (testBit(lcdControl,3))
			bgTileMapLocation = 0x9C00;
		else
			bgTileMapLocation = 0x9800;

		if (testBit(lcdControl,2)) spriteHeight=16;
		
		// Set tile data pointer.
		// Bit 4 - BG & Window Tile Data Select (0=8800-97FF, 1=8000-8FFF)
		//if ((lcdControl & (1 << 4)) == 0) {
		
		if (testBit(lcdControl,4)==false) { 
			tileDataPtr = 0x8800;
			signedTileIndices = true;
		} else {
			tileDataPtr = 0x8000;
			signedTileIndices = false;
		}
		

        // Bit 6 - Window Screen Display Data Select
        int windowTileMapLocation = 0;
		if (testBit(lcdControl,6))
			windowTileMapLocation = 0x9C00;
		else
			windowTileMapLocation = 0x9800;

		// if(GPU._bgtile == 1 && tile < 128) tile += 256;
		// int tileNum = signedTileNumbers ? ((int)bTileNum + 128) : (bTileNum & 0xff);

		int scrolly = cpu.mem.RAM[0xFF42];
		int scrollx = cpu.mem.RAM[0xFF43];
		int windowYPosition = cpu.mem.RAM[0xFF4A];
		int windowXPosition = cpu.mem.RAM[0xFF4B]-7;
		
		int xx, yy;

		yy = y + scrolly; // + bd.mouseY();
		int subx = 0, suby = yy % 8;

		for (int x = 0; x < 160; x++) {
			xx = x + scrollx; // + bd.mouseX();
			
			// Wrap xx and yy
			xx=xx&(0xff);
			yy=yy&(0xff);
			
			int charOffset = (xx / 8) + ((yy / 8) * 32);
			int charIndex = (byte) cpu.mem.peek(bgTileMapLocation + charOffset);
			
			int cgbTileAttributes = cpu.mem.VRAMBANK1[0x1800 + charOffset]; 
			int tileVramBank = ((cgbTileAttributes&(1<<3))>0)?1:0;

			if (signedTileIndices)
				charIndex += 128;
			else
				charIndex = charIndex & 0xff;

			subx = xx % 8;

			int pix = 0;
			
			if (cpu.hardwareType==HARDWARE_TYPE.DMG1) {
				pix = getTilePixel(cpu, charIndex, subx, suby, tileVramBank);
				// Set draw colour from pixel data.
				bd.setDrawColor(backgroundPaletteMap[(pix)&3]);
			} else if (cpu.hardwareType==HARDWARE_TYPE.CGB) {
				int subxf = subx;
				int subyf = suby;
				if (testBit(cgbTileAttributes,5)) subxf = 7 - subxf;
				if (testBit(cgbTileAttributes,6)) subyf = 7 - subyf;
				pix = getTilePixel(cpu, charIndex, subxf, subyf, tileVramBank);
				int cgbTilePal = ((cgbTileAttributes)&0x7)*4;
				bd.setDrawColor(cgbBackgroundColors[cgbTilePal+((pix)&3)]);
			}
			
			// Handle Window
			if (windowEnabled && x>=windowXPosition && y>=windowYPosition) {
				int windowInsideX = x-windowXPosition;
				int windowInsideY = y-windowYPosition;
				charOffset = (windowInsideX / 8) + ((windowInsideY / 8) * 32);
				charIndex = (byte) cpu.mem.RAM[windowTileMapLocation + charOffset];
				
				if (signedTileIndices)
					charIndex += 128;
				else
					charIndex = charIndex & 0xff;
				
				//charIndex = charIndex & 0xff;
				pix = getTilePixel(cpu, charIndex, windowInsideX%8, windowInsideY%8, tileVramBank);
				bd.setDrawColor(backgroundPaletteMap[pix&3]);
			}

			// Sprites
			// Bit6   Y flip          (0=Normal, 1=Vertically mirrored)
			// Bit5   X flip          (0=Normal, 1=Horizontally mirrored)
			int sprPalette = 0;
			if (spritesEnabled) {
				for (int i=0;i<40;i++) {
					if (x>=sprites[i].x && x<sprites[i].x+8) {
						if (y>=sprites[i].y && y<sprites[i].y+spriteHeight) {
							//bd.setDrawColor(Color.GREEN);
							int sprsubx = (x-sprites[i].x)&7;
							int sprsuby = (y-sprites[i].y)&(spriteHeight-1);
							int sprAttributes = sprites[i].attributes;
							if (((sprAttributes)&(1<<5))>0) sprsubx=7-sprsubx;
							if (((sprAttributes)&(1<<6))>0) sprsuby=(spriteHeight-1)-sprsuby;
							if (((sprAttributes)&(1<<4))>0) sprPalette=1; // Bit4: Palette number
							
							int sprPixel = getSpritePixel2(cpu, sprites[i].tileId, sprsubx, sprsuby);
							
							if (sprPixel!=0)
								bd.setDrawColor(getSpriteCol(sprPixel, sprPalette));
							
							// HACK! quick test of CGB palettes.
							if (cpu.hardwareType==HARDWARE_TYPE.CGB && sprPixel!=0) {
								int cgbTilePal = sprites[i].cgbSpritePalette*4;
								bd.setDrawColor(cgbSpriteColors[cgbTilePal+((sprPixel)&3)]);
							}
						}
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
        sprites[id].cgbSpritePalette = cpu.mem.RAM[spriteAddress+3]&0x7;
	}
        
}

class Sprite {
	int x,y,tileId,attributes,cgbSpritePalette;
}

