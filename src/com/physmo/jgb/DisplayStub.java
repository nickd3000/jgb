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
 */
public class DisplayStub {
	public static final int scale = 2;
	
	public static final Color c1 = new Color(0, 0, 0);
	public static final Color c2 = new Color(70,70, 70);
	public static final Color c3 = new Color(180, 180, 180);
	public static final Color c4 = new Color(255, 255, 255);
	
	//public static int scanline = 0;
	
	public static void render(CPU cpu, BasicDisplay bd) {
		int y = cpu.mem.RAM[0xFF44]; // Scanline register

		if (y < 144)
			renderLine(cpu, bd, y);
		y = y + 1;

		if (y == 144) {
			cpu.mem.RAM[0xFF0F] |= CPU.INT_VBLANK;
		}

		if (y == 0xff) {
			y = 0;

			bd.refresh();
		}

		cpu.mem.RAM[0xFF44] = y;
				
		
	}
	
	static int tileDataPtr = 0x8800;
	
	public static int getTilePixel(CPU cpu,int tileId, int subx, int suby) {
		int byteOffset = (suby * 2)+(subx/4);
		int data = cpu.mem.RAM[tileDataPtr+byteOffset];
		int nibble = (subx/2)&3;
		switch (nibble) {
		case 0:
			return data&3;
		case 1:
			return (data>>2)&3;
		case 2:
			return (data>>4)&3;
		case 3:
			return (data>>6)&3;
		}
		
		return 0;
	}
	
	public static int getTilePixel2(CPU cpu,int tileId, int subx, int suby) {
		int byteOffset = (suby * 2);;
		
		int data1 = cpu.mem.RAM[tileDataPtr+(tileId*16)+byteOffset];
		int data2 = cpu.mem.RAM[tileDataPtr+(tileId*16)+byteOffset+1];
		int mask = 1<<(7-subx);
		int color = 0;
		if ((data1&mask)>0) color+=0b0001;
		if ((data2&mask)>0) color+=0b0010;
		
		
		return color;
	}
	
	public static void renderLine(CPU cpu, BasicDisplay bd, int y) {
		//int charOffset = (x >> 3) + ((y >> 3) * 40);
		//int charIndex = cpu.RAM[ADDR_SCREEN_RAM + charOffset];
		//160x144
		int vram = 0x8010;
		//int vram = 0x9800; // Tilemap 0
		//int vram = 0x9C00; // Tilemap 1
		
		int lcdControl = cpu.mem.RAM[0xFF40];
		
		// Bit 3 - BG Tile Map Display Select     (0=9800-9BFF, 1=9C00-9FFF)
		if ((lcdControl&(1<<3))==0) vram=0x9800;
		else vram=0x9C00;
		
		// Set tile data pointer.
		// Bit 4 - BG & Window Tile Data Select   (0=8800-97FF, 1=8000-8FFF)
		if ((lcdControl&(1<<4))==0) tileDataPtr = 0x8800;
		else tileDataPtr = 0x8000;
		
		
		int scrolly = cpu.mem.RAM[0xFF42];
		int scrollx = cpu.mem.RAM[0xFF43];
		int xx,yy;
		int currentTile = 3;
		yy = y+scrolly;
		int subx=0,suby=yy%8;
		
		for (int x=0;x<160;x++) {
			xx = x+scrollx;
			int charOffset = (xx / 8) + ((yy / 8) * 32);
			int charIndex = cpu.mem.RAM[vram + charOffset];
			subx=xx%8;
			
			int tp = getTilePixel2(cpu,charIndex,subx,suby);
			
			int pix=tp; //+charIndex;
					
			if ((pix&3)==0) 
				bd.setDrawColor(c4);
			else if ((pix&3)==1)
				bd.setDrawColor(c3);
			else if ((pix&3)==2)
				bd.setDrawColor(c2);
			else if ((pix&3)==3)
				bd.setDrawColor(c1);
			
			bd.drawFilledRect(x * scale, y * scale, scale, scale);
		}
	}
}
