package com.physmo.jgb;

public class Glow {
    static int width = 160;
    static int height = 144;
    static int [] glowGrid = new int[width*height];
    static int maxBrightness = 100;
    public static void clear() {
        double amount = 0.98;
        int scale = (int)(0xff*amount);
        for (int i=0;i<width*height;i++) {
            glowGrid[i]=0;
            //glowGrid[i] = (glowGrid[i]*scale)/0xff;
        }
    }

    public static void addSprite(int x, int y) {
        drawBlob(x+2,y+4,20,45);
    }

    public static int getPoint(int x, int y) {
        return glowGrid[x+(width*y)];
    }

    public static int adjustDrawColorFromPoint(int x, int y, int c) {

        int glow =  glowGrid[x+(width*y)];
        if (glow==0) return c;

        int r = (c>>16)&0xff;
        int g = (c>>8)&0xff;
        int b = (c)&0xff;
        r+=glow;
        g+=glow;
        b+=glow;
        if (r>0xff) r=0xff;
        if (g>0xff) g=0xff;
        if (b>0xff) b=0xff;

        int cc = (0xff<<24)+(r<<16)+(g<<8)+(b);

        return cc;
    }

    public static int clampy(int y) {
        if (y<0) return 0;
        if (y>height) return height;
        return y;
    }
    public static int clampx(int x) {
        if (x<0) return 0;
        if (x>width) return width;
        return x;
    }

    public static void drawBlob(int mx, int my, int r, int v) {
        double d=0;
        double dx,dy;
        double dr = (double)r;
        double dv = (double)v;
        int index;


        for (int y=clampy(my-r); y<clampy(my+r); y++){
            for (int x=clampx(mx-r); x<clampx(mx+r); x++){

                dx = x-mx; dy = y-my;
                d = Math.sqrt((dx*dx)+(dy*dy));
                if (d>r) continue;
                d=(dr - d)/dr; // Invert and normalise.
                d*=dv;
                index = x+(width*y);
                glowGrid[index]+=d;
                if (glowGrid[index]>maxBrightness) {
                    glowGrid[index] = maxBrightness;
                }
            }
        }
    }

}
