package com.physmo.jgb;


import com.physmo.minvio.BasicDisplay;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
//import com.physmo.toolbox.BasicDisplay;


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
    static int spriteDataPtr = 0x8000;
    private static int tileDataPtr = 0x8800;
    public final int[] cgbBackgroundPaletteData = new int[64];
    public final int[] cgbSpritePaletteData = new int[64];
    private final Sprite[] sprites = new Sprite[40];
    private final PALETTE_TYPE paletteType = PALETTE_TYPE.DMG;
    private final int[] backgroundPaletteMaster = new int[4];
    private final int[] sprite1PaletteMaster = new int[4];
    // TODO: set these up in a method
    private final int[] sprite2PaletteMaster = new int[4];
    private final int[] backgroundPaletteMap = new int[4];
    private final int[] sprite1PaletteMap = new int[4];
    private final int[] sprite2PaletteMap = new int[4];
    private final int[] cgbBackgroundColors = new int[64];
    private final int[] cgbSpriteColors = new int[64];
    int drawBufferWidth = 160;
    int drawBufferHeight = 144;
    BufferedImage drawBuffer = new BufferedImage(drawBufferWidth, drawBufferHeight, BufferedImage.TYPE_INT_ARGB);
    private int scale = 3;
    private int clock = 0;
    private int currentMode = 0;
    private int frameCount = 0;
    // public static int scanline = 0;
    private int lastLineRendered = 0;

    public GPU(int scale) {
        this.scale = scale;
        for (int i = 0; i < 40; i++) {
            sprites[i] = new Sprite();
        }
        for (int i = 0; i < 64; i++) {
            cgbBackgroundPaletteData[i] = (i * 2) & 0xff;
            cgbSpritePaletteData[i] = (i * 2) & 0xff;
        }


        PaletteGenerator.setPalettes(this, PALETTE_TYPE.LIGHT);
    }

    public int[] getCgbBackgroundPaletteData() {
        return cgbBackgroundPaletteData;
    }

    public int[] getCgbSpritePaletteData() {
        return cgbSpritePaletteData;
    }


    public int[] getBackgroundPaletteMaster() {
        return backgroundPaletteMaster;
    }

    public int[] getSprite1PaletteMaster() {
        return sprite1PaletteMaster;
    }

    public int[] getSprite2PaletteMaster() {
        return sprite2PaletteMaster;
    }

//    public void setGBCColorManually(int[] colorList, int index, int hexColor) {
//        int r = ((hexColor >> 16) & 0xff) >> 3;
//        int g = ((hexColor >> 8) & 0xff) >> 3;
//        int b = ((hexColor & 0xff)) >> 3;
//        int combined = (r << 10) | (g << 5) | b;
//        colorList[index * 2] = combined & 0xff;
//        colorList[(index * 2) + 1] = (combined >> 8) & 0xff;
//    }

    public void debug(CPU cpu, BasicDisplay bd) {
        bd.setDrawColor(Color.GREEN);
        bd.drawText("FL: " + Utils.toHex2(cpu.FL), 350 - 100, 50);
    }

    public void tick(CPU cpu, BasicDisplay bd, int cycles) {

        int previousMode = currentMode;
        int y = cpu.mem.RAM[MEM.ADDR_FF44_SCANLINE]; // Scanline register
        int lcdStat = cpu.mem.RAM[MEM.ADDR_FF41_LCD_STAT];
        int lcdControl = cpu.mem.RAM[0xFF40];

        boolean lcdEnabled = testBit(lcdControl, 7);

        // Handle LCD OFF
        if (!lcdEnabled) {
            cpu.mem.RAM[MEM.ADDR_FF44_SCANLINE] = 0;
            cpu.mem.RAM[0xFF41] &= 0b11111100;

            if (previousMode != 0 && testBit(lcdStat, 3)) {
                cpu.requestInterrupt(CPU.INT_LCDSTAT);
            }

            setLCDRegisterMode(cpu, 0);
            clock = 0;
            return;
        }

        clock = (clock + cycles) & 0xFFFFFFFA;

        boolean modeInterruptEnabled = false;

        if (y >= 144) {
            currentMode = 1;

            if (testBit(lcdStat, 4))
                modeInterruptEnabled = true;

        } else {
            if (clock <= 80) {
                // Mode 2
                currentMode = 2;

                if (testBit(lcdStat, 5))
                    modeInterruptEnabled = true;

            } else if (clock >= 80 && clock < 252) {
                // Mode 3
                currentMode = 3;
                //setLCDRegisterMode(cpu, 3);
            } else if (clock >= 252 && clock < 456) {
                // Mode 0
                currentMode = 0;

                if (testBit(lcdStat, 3))
                    modeInterruptEnabled = true;

            }
        }

        setLCDRegisterMode(cpu, currentMode);
        if (currentMode != previousMode && modeInterruptEnabled) {
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
                frameCount++;

                //debugDrawYIncidence(cpu,bd);


                bd.drawImage(drawBuffer, 0, 0, drawBufferWidth * scale, drawBufferHeight * scale);
                bd.repaint(60);

            }
        }


        // Handle y coincidence check.
        //int ycompare = cpu.mem.peek(CPU.ADDR_FF45_Y_COMPARE);
        int ycompare = cpu.mem.RAM[MEM.ADDR_FF45_Y_COMPARE];
        if (y == ycompare) {
            cpu.mem.RAM[MEM.ADDR_FF41_LCD_STAT] |= 0x04;
            if ((cpu.mem.RAM[MEM.ADDR_FF41_LCD_STAT] & (1 << 6)) > 0) {
                cpu.requestInterrupt(CPU.INT_LCDSTAT);
            }
        } else {
            cpu.mem.RAM[MEM.ADDR_FF41_LCD_STAT] &= ~0x04;
        }

        //debugDrawYIncidence(cpu,bd);

        cpu.mem.RAM[MEM.ADDR_FF44_Y_SCANLINE] = y;
        //cpu.mem.poke(0xFF44,y);
    }

    private void setLCDRegisterMode(CPU cpu, int mode) {
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

    private boolean testBit(int value, int bit) {
        return (value & (1 << bit)) > 0;
    }

    private void renderLine(CPU cpu, BasicDisplay bd, int y) {

        int bgTileMapLocation = 0x8010;
        int[] pixels = ((DataBufferInt) drawBuffer.getRaster().getDataBuffer()).getData();
        int drawColor = 0x00ff00;

        processPalettes(cpu);
        processGBCPalettes(cpu);
        getSprites(cpu);

        boolean signedTileIndices;
        int lcdControl = cpu.mem.RAM[0xFF40];
        int spriteHeight = 8;

        // Bit 1 - OBJ (Sprite) Display Enable    (0=Off, 1=On)
        boolean spritesEnabled = testBit(lcdControl, 1);

        // Bit 5 - Window Display Enable          (0=Off, 1=On)
        boolean windowEnabled = testBit(lcdControl, 5);


        // Bit 3 - BG Tile Map Display Select (0=9800-9BFF, 1=9C00-9FFF)
        if (testBit(lcdControl, 3))
            bgTileMapLocation = 0x1C00;
        else
            bgTileMapLocation = 0x1800;

        if (testBit(lcdControl, 2)) spriteHeight = 16;

        // Set tile data pointer.
        // Bit 4 - BG & Window Tile Data Select (0=8800-97FF, 1=8000-8FFF)
        //if ((lcdControl & (1 << 4)) == 0) {

        if (!testBit(lcdControl, 4)) {
            tileDataPtr = 0x8800;
            signedTileIndices = true;
        } else {
            tileDataPtr = 0x8000;
            signedTileIndices = false;
        }


        // Bit 6 - Window Screen Display Data Select
        int windowTileMapLocation = 0;
        if (testBit(lcdControl, 6))
            windowTileMapLocation = 0x9C00 - 0x8000;
        else
            windowTileMapLocation = 0x9800 - 0x8000;

        // if(GPU._bgtile == 1 && tile < 128) tile += 256;
        // int tileNum = signedTileNumbers ? ((int)bTileNum + 128) : (bTileNum & 0xff);

        int scrolly = cpu.mem.RAM[0xFF42];
        int scrollx = cpu.mem.RAM[0xFF43];
        int windowYPosition = cpu.mem.RAM[0xFF4A];
        int windowXPosition = cpu.mem.RAM[0xFF4B] - 7;

        int xx, yy;

        yy = y + scrolly; // + bd.mouseY();
        int subx = 0, suby = yy % 8;

        for (int x = 0; x < 160; x++) {
            xx = x + scrollx; // + bd.mouseX();

            // Wrap xx and yy
            xx = xx & (0xff);
            yy = yy & (0xff);

            int charOffset = (xx / 8) + ((yy / 8) * 32);

            int charIndex = (byte) cpu.mem.VRAMBANK0[bgTileMapLocation + charOffset];

            int cgbTileAttributes = cpu.mem.VRAMBANK1[bgTileMapLocation + charOffset];
            int tileVramBank = ((cgbTileAttributes & (1 << 3)) > 0) ? 1 : 0;
            //tileVramBank = 1;
            //tileVramBank = ((int)(Math.random()*33.0))&1;

			/*
			 * 		// Bit 3 - BG Tile Map Display Select (0=9800-9BFF, 1=9C00-9FFF)
				if (testBit(lcdControl,3))
					bgTileMapLocation = 0x1C00;
				else
					bgTileMapLocation = 0x1800;
			 */

            if (signedTileIndices)
                charIndex += 128;
            else
                charIndex = charIndex & 0xff;

            subx = xx % 8;

            int pix = 0;

            if (cpu.hardwareType == HARDWARE_TYPE.DMG1) {
                pix = getTilePixel(cpu, charIndex, subx, suby, tileVramBank);
                drawColor = backgroundPaletteMap[(pix) & 3];

            } else if (cpu.hardwareType == HARDWARE_TYPE.CGB) {
                int subxf = subx;
                int subyf = suby;
                if (testBit(cgbTileAttributes, 5)) subxf = 7 - subxf;
                if (testBit(cgbTileAttributes, 6)) subyf = 7 - subyf;
                pix = getTilePixel(cpu, charIndex, subxf, subyf, tileVramBank);
                int cgbTilePal = ((cgbTileAttributes) & 0x7) * 4;
                drawColor = cgbBackgroundColors[cgbTilePal + ((pix) & 3)];
            }

            // Handle Window
            if (windowEnabled && x >= windowXPosition && y >= windowYPosition) {
                int windowInsideX = x - windowXPosition;
                int windowInsideY = y - windowYPosition;
                charOffset = (windowInsideX / 8) + ((windowInsideY / 8) * 32);

                //charIndex = (byte) cpu.mem.RAM[windowTileMapLocation + charOffset];
                charIndex = (byte) cpu.mem.VRAMBANK0[windowTileMapLocation + charOffset];

                cgbTileAttributes = cpu.mem.VRAMBANK1[windowTileMapLocation + charOffset];
                //tileVramBank = ((cgbTileAttributes&(1<<3))>0)?1:0;

                if (signedTileIndices)
                    charIndex += 128;
                else
                    charIndex = charIndex & 0xff;

                //charIndex = charIndex & 0xff;
                if (cpu.hardwareType == HARDWARE_TYPE.DMG1) {
                    pix = getTilePixel(cpu, charIndex, windowInsideX % 8, windowInsideY % 8, tileVramBank);
                    drawColor = backgroundPaletteMap[pix & 3];
                } else if (cpu.hardwareType == HARDWARE_TYPE.CGB) {
                    int subxf = windowInsideX % 8;
                    int subyf = windowInsideY % 8;
                    if (testBit(cgbTileAttributes, 5)) subxf = 7 - subxf;
                    if (testBit(cgbTileAttributes, 6)) subyf = 7 - subyf;
                    //pix = getTilePixel(cpu, charIndex, subxf, subyf, tileVramBank);
                    tileVramBank = 0;
                    pix = getTilePixel(cpu, charIndex, subxf, subyf, tileVramBank);
                    int cgbTilePal = ((cgbTileAttributes) & 0x7) * 4;
                    drawColor = cgbBackgroundColors[cgbTilePal + ((pix) & 3)];
                }
            }

            // Glow
            //drawColor = Glow.adjustDrawColorFromPoint( x,y, drawColor);

            // Sprites
            // Bit6   Y flip          (0=Normal, 1=Vertically mirrored)
            // Bit5   X flip          (0=Normal, 1=Horizontally mirrored)
            int sprPalette = 0;
            int sprBank = 0;
            if (spritesEnabled) {
                for (int i = 0; i < 40; i++) {
                    if (x >= sprites[i].x && x < sprites[i].x + 8) {
                        if (y >= sprites[i].y && y < sprites[i].y + spriteHeight) {
                            //bd.setDrawColor(Color.GREEN);
                            int sprsubx = (x - sprites[i].x) & 7;
                            int sprsuby = (y - sprites[i].y) & (spriteHeight - 1);
                            int sprAttributes = sprites[i].attributes;
                            sprBank = 0;
                            if (testBit(sprAttributes, 5)) sprsubx = 7 - sprsubx;
                            if (testBit(sprAttributes, 6)) sprsuby = (spriteHeight - 1) - sprsuby;
                            if (testBit(sprAttributes, 4)) sprPalette = 1; // Bit4: Palette number
                            if (testBit(sprAttributes, 3)) sprBank = 1; // Bit4: Palette number

                            int sprPixel = getSpritePixel2(cpu, sprites[i].tileId, sprsubx, sprsuby, sprBank);

                            if (sprPixel != 0) {
                                if (cpu.hardwareType == HARDWARE_TYPE.DMG1) {
                                    drawColor = getSpriteCol(sprPixel, sprPalette);
                                } else {
                                    int cgbTilePal = sprites[i].cgbSpritePalette * 4;
                                    drawColor = cgbSpriteColors[cgbTilePal + ((sprPixel) & 3)];
                                }
                            }

                        }
                    }
                }
            }

            //BufferedImage drawBuffer = (BufferedImage)(bd.getDrawBuffer());

            //drawColor|=0xff000000;
            pixels[x + (y * drawBufferWidth)] = drawColor; //.getRGB();


            //bd.drawFilledRect(x * scale, y * scale, scale, scale);
        }
        //bd.drawImage(drawBuffer,0,0);
    }

    private void processPalettes(CPU cpu) {
        //int bgPalette = cpu.mem.RAM[MEM.ADDR_0xFF47_BGPALETTE];
        int bgPalette = cpu.mem.peek(MEM.ADDR_FF47_BGPALETTE);

        backgroundPaletteMap[0] = backgroundPaletteMaster[(bgPalette & 0b11)];
        backgroundPaletteMap[1] = backgroundPaletteMaster[((bgPalette >> 2) & 0b11)];
        backgroundPaletteMap[2] = backgroundPaletteMaster[((bgPalette >> 4) & 0b11)];
        backgroundPaletteMap[3] = backgroundPaletteMaster[((bgPalette >> 6) & 0b11)];

        //int sprPalette1 = cpu.mem.RAM[MEM.ADDR_0xFF48_SPRITEPALETTE1];
        int sprPalette1 = cpu.mem.peek(MEM.ADDR_FF48_SPRITEPALETTE1);
        sprite1PaletteMap[0] = sprite1PaletteMaster[(sprPalette1 & 3)];
        sprite1PaletteMap[1] = sprite1PaletteMaster[((sprPalette1 >> 2) & 3)];
        sprite1PaletteMap[2] = sprite1PaletteMaster[((sprPalette1 >> 4) & 3)];
        sprite1PaletteMap[3] = sprite1PaletteMaster[((sprPalette1 >> 6) & 3)];

        //int sprPalette2 = cpu.mem.RAM[MEM.ADDR_0xFF49_SPRITEPALETTE2];
        int sprPalette2 = cpu.mem.peek(MEM.ADDR_FF49_SPRITEPALETTE2);
        sprite2PaletteMap[0] = sprite2PaletteMaster[(sprPalette2 & 3)];
        sprite2PaletteMap[1] = sprite2PaletteMaster[((sprPalette2 >> 2) & 3)];
        sprite2PaletteMap[2] = sprite2PaletteMaster[((sprPalette2 >> 4) & 3)];
        sprite2PaletteMap[3] = sprite2PaletteMaster[((sprPalette2 >> 6) & 3)];
    }

    private void processGBCPalettes(CPU cpu) {
        int col = 0;
        for (int i = 0; i < 64; i += 2) {
            cgbBackgroundColors[col] = createColorFrom2ByteValue(
                    cgbBackgroundPaletteData[i],
                    cgbBackgroundPaletteData[i + 1]);

            cgbSpriteColors[col] = createColorFrom2ByteValue(
                    cgbSpritePaletteData[i],
                    cgbSpritePaletteData[i + 1]);
            col++;
        }
    }

    // Bit 0-4   Red Intensity   (00-1F)
    // Bit 5-9   Green Intensity (00-1F)
    // Bit 10-14 Blue Intensity  (00-1F)
    private int createColorFrom2ByteValue(int b2, int b1) {
        int alpha = 0xff;
        int scale = 7;
        int combined = ((b1 & 0xff) << 8) | (b2 & 0xff);
        int r = (combined) & 0x1f;
        int g = (combined >> 5) & 0x1f;
        int b = (combined >> 10) & 0x1f;
        //return (0xff<<24)+(r<<16)+(g<<8)+b;
        //return new Color(r*scale,g*scale,b*scale);

        float[] hsv = new float[4];
        Color.RGBtoHSB(r * scale, g * scale, b * scale, hsv);
        hsv[1] *= 0.6; // Scale saturation down a bit.
        int newColInt = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]);

        return newColInt;

        //return (alpha<<24)+((r*scale)<<16)+((g*scale)<<8)+(b*scale);
    }

    private int getSpritePixel2(CPU cpu, int tileId, int subx, int suby, int bank) {
        int byteOffset = (suby * 2);

        int data1, data2 = 0;
        if (bank == 0) {
            data1 = cpu.mem.VRAMBANK0[(tileId * 16) + byteOffset];
            data2 = cpu.mem.VRAMBANK0[(tileId * 16) + byteOffset + 1];
        } else {
            data1 = cpu.mem.VRAMBANK1[(tileId * 16) + byteOffset];
            data2 = cpu.mem.VRAMBANK1[(tileId * 16) + byteOffset + 1];
        }

        int mask = 1 << (7 - subx);
        int color = 0;
        if ((data1 & mask) > 0)
            color += 0b0001;
        if ((data2 & mask) > 0)
            color += 0b0010;

        return color;
    }

    private int getTilePixel(CPU cpu, int tileId, int subx, int suby, int bank) {
        int byteOffset = (suby * 2);

        int normalisedPointer = tileDataPtr - 0x8000;

        int data1 = 0, data2 = 0;

        if (bank == 0) {
            data1 = cpu.mem.VRAMBANK0[normalisedPointer + (tileId * 16) + byteOffset];
            data2 = cpu.mem.VRAMBANK0[normalisedPointer + (tileId * 16) + byteOffset + 1];
        } else {
            data1 = cpu.mem.VRAMBANK1[normalisedPointer + (tileId * 16) + byteOffset];
            data2 = cpu.mem.VRAMBANK1[normalisedPointer + (tileId * 16) + byteOffset + 1];
        }


        int mask = 1 << (7 - subx);
        int color = 0;
        if ((data1 & mask) > 0)
            color |= 0b01;
        if ((data2 & mask) > 0)
            color |= 0b10;

        return color;
    }

    private int getSpriteCol(int i, int pal) {

        if (pal == 0) {
            return sprite1PaletteMap[i & 3];
        } else {
            return sprite2PaletteMap[i & 3];
        }

    }

    private void getSprites(CPU cpu) {
        for (int i = 0; i < 40; i++) {
            getSprite(cpu, i);
        }
    }

    private void getSprite(CPU cpu, int id) {
        int spriteAddress = (0xFE00) + (id * 4);
        int[] sprData = cpu.mem.RAM;
        sprites[id].y = sprData[spriteAddress] - 16; // Offset for display window.
        sprites[id].x = sprData[spriteAddress + 1] - 8; // Offset for display window.
        sprites[id].attributes = sprData[spriteAddress + 3];
        sprites[id].cgbSpritePalette = sprData[spriteAddress + 3] & 0x7;
        sprites[id].tileId = sprData[spriteAddress + 2];

        //if (sprites[id].y<50) sprites[id].y+=50;
    }

    private void debugDrawYIncidence(CPU cpu, BasicDisplay bd) {
        int ycmp = cpu.mem.RAM[MEM.ADDR_FF45_Y_COMPARE];
        bd.setDrawColor(Color.RED);
        bd.drawFilledRect(0, (ycmp - 2) * 2, 5, 4 * 2);
    }

    private void debugDrawKeyStates(CPU cpu, BasicDisplay bd, int x, int y, Color col) {

        bd.setDrawColor(col);

//		for (int i=0;i<cpu.input.keyState.length;i++) {
//			bd.drawText(cpu.input.keyState[i]==0?"---":"-1-", x, y+(i*15));
//		}

        ///
        bd.setDrawColor(Color.yellow);

        int lcdControl = cpu.mem.RAM[0xFF40];
        int bgTileMapLocation = 0;
        boolean signedTileIndices;
        // Bit 3 - BG Tile Map Display Select (0=9800-9BFF, 1=9C00-9FFF)
        if (testBit(lcdControl, 3))
            bgTileMapLocation = 0x9C00 - 0x8000;
        else
            bgTileMapLocation = 0x9800 - 0x8000;

        if (!testBit(lcdControl, 4)) {
            tileDataPtr = 0x8800;
            signedTileIndices = true;
        } else {
            tileDataPtr = 0x8000;
            signedTileIndices = false;
        }

        bd.drawText("lcdControl = " + Utils.toHex2(lcdControl), 10, 300);
        bd.drawText("bgTileMapLocation = " + Utils.toHex2(bgTileMapLocation), 10, 320);
        bd.drawText("tileDataPtr = " + Utils.toHex2(tileDataPtr), 10, 340);
        bd.drawText("signedTileIndices = " + signedTileIndices, 10, 360);
    }
}

