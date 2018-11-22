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
	
	
 */
public class DisplayStub {
	public static final int scale = 2;
	
	public static final Color c1 = new Color(0, 0, 0);
	public static final Color c2 = new Color(70,70, 70);
	public static final Color c3 = new Color(180, 180, 180);
	public static final Color c4 = new Color(255, 255, 255);
	
	//public static int scanline = 0;
	
	public static void render(CPU cpu, BasicDisplay bd) {
		int y = cpu.mem.RAM[0xFF44];

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
	
	public static int getTilePixel(CPU cpu,int tileId, int subx, int suby) {
		
		// first char row takes two bytes
		
		int byteOffset = (suby * 2)+(subx/4);
		int data = cpu.mem.RAM[0x8800+byteOffset];
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
	
	public static void renderLine(CPU cpu, BasicDisplay bd, int y) {
		//int charOffset = (x >> 3) + ((y >> 3) * 40);
		//int charIndex = cpu.RAM[ADDR_SCREEN_RAM + charOffset];
		//160x144
		//int vram = 0x8010;
		int vram = 0x9800; // Tilemap 0
		//int vram = 0x9C00; // Tilemap 1
		
		int currentTile = 3;
		int subx=0,suby=y%8;
		for (int x=0;x<160;x++) {
			int charOffset = (x / 8) + ((y / 8) * 20);
			int charIndex = cpu.mem.RAM[vram + charOffset];
			subx=x%8;
			
			int tp = getTilePixel(cpu,charIndex,subx,suby);
			
			int pix=tp+charIndex;
					
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
