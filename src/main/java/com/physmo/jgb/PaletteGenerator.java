package com.physmo.jgb;

import java.awt.Color;

public class PaletteGenerator {

    public static void setPalettes(GPU gpu, PALETTE_TYPE type) {

        int c1 = 0, c2 = 0, c3 = 0, c4 = 0;

        switch (type) {
            case DMG:
                c1 = buildColor(new Color(0x7a8110));
                c2 = buildColor(new Color(0x5a7942));
                c3 = buildColor(new Color(0x385849));
                c4 = buildColor(new Color(0x294039));
                break;
            case POCKET:
                c1 = buildColor(new Color(0xc5cba5));
                c2 = buildColor(new Color(0x8c926a));
                c3 = buildColor(new Color(0x4a5139));
                c4 = buildColor(new Color(0x181818));
                break;
            case LIGHT:
                c1 = buildColor(new Color(0x00b284));
                c2 = buildColor(new Color(0x009a73));
                c3 = buildColor(new Color(0x006849));
                c4 = buildColor(new Color(0x005139));
                break;
        }


        setGPUPalette(gpu.getCgbBackgroundPaletteData(), c1, c2, c3, c4, false);
        setGPUPalette(gpu.getCgbSpritePaletteData(), c1, c2, c3, c4, false);
        setGPUPalette(gpu.getCgbSpritePaletteData(), c1, c2, c3, c4, true);
    }

    public static int buildColor(Color c) {
        int alpha = 0xff;
        //return ((c.getRed())<<24)+((c.getGreen()&0xff)<<16)+((c.getBlue()&0xff)<<8)+((c.getAlpha()));
        return (((alpha) << 24)) +
                ((c.getRed() & 0xff) << 16) +
                ((c.getGreen() & 0xff) << 8) +
                ((c.getBlue() & 0xff));
    }

    public static void setGPUPalette(int[] palette, int c1, int c2, int c3, int c4, boolean sprites2) {
        int shift = 0;
        if (sprites2) shift = 4;
        setGBCColorManually(palette, shift + 0, c1);
        setGBCColorManually(palette, shift + 1, c2);
        setGBCColorManually(palette, shift + 2, c3);
        setGBCColorManually(palette, shift + 3, c4);

    }

    private static void setGBCColorManually(int[] colorList, int index, int rgb) {
        // 5 bit components
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = (rgb) & 0xff;
        int rr = r >> 3;
        int gg = g >> 3;
        int bb = b >> 3;
        int combined = ((bb & 0x1f) << 10) | ((gg & 0x1f) << 5) | ((rr & 0x1f));
        colorList[index * 2] = combined & 0xff;
        colorList[(index * 2) + 1] = (combined >> 8) & 0xff;
    }

}
