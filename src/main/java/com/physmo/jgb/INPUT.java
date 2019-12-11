package com.physmo.jgb;

import com.physmo.minvio.BasicDisplay;

public class INPUT {

    public static int A = 0;
    public static int B = 1;
    public static int START = 2;
    public static int SELECT = 3;
    public static int UP = 4;
    public static int DOWN = 5;
    public static int LEFT = 6;
    public static int RIGHT = 7;
    public int[] keyMap = {90, // Z
            88, // X
            10, 32, // start / select
            38, 40, 37, 39}; // Arrows
    public int[] keyState = {0, 0, 0, 0, 0, 0, 0, 0};
    public int[] keyPrevious = {0, 0, 0, 0, 0, 0, 0, 0};
    CPU cpu = null;

    public INPUT(CPU cpu) {
        this.cpu = cpu;
    }

    public void tick(BasicDisplay bd) {
        // z=90 x=88 up:38 down:40 left:37 right:39
        // space:32 return:10

        // Poll keys.
        for (int i = 0; i < keyMap.length; i++) {
            int keyCode = keyMap[i];
            if (bd.getKeyState()[keyCode] > 0) {
                keyState[i] = 1;
            } else {
                keyState[i] = 0;
            }
        }

        // Handle interrupts.
        for (int i = 0; i < keyMap.length; i++) {
            if (keyPrevious[i] == 10 && keyState[i] == 1) {

                // Should we only respond to the signals the cpu is looking for?
                if (i <= 3 && (cpu.mem.RAM[0xFF00] & 0x10) > 0) {
                    System.out.println("jp int");
                    cpu.requestInterrupt(CPU.INT_JOYPAD);
                }

                if (i >= 4 && (cpu.mem.RAM[0xFF00] & 0x20) > 0) {
                    System.out.println("jp int");
                    cpu.requestInterrupt(CPU.INT_JOYPAD);
                }

            }
        }

        // Now copy new state to previous state.
        System.arraycopy(keyState, 0, keyPrevious, 0, keyMap.length);
    }


    public void pokeFF00(int val) {
        cpu.mem.RAM[0xFF00] = val & 0x30;

        //this.register.p1 = byte&0x30;
    }

    /*
             * Bit 7 - Not used
        Bit 6 - Not used
        Bit 5 - P15 Select Button Keys (0=Select)
        Bit 4 - P14 Select Direction Keys (0=Select)
        Bit 3 - P13 Input Down or Start (0=Pressed) (Read Only)
        Bit 2 - P12 Input Up or Select (0=Pressed) (Read Only)
        Bit 1 - P11 Input Left or Button B (0=Pressed) (Read Only)
        Bit 0 - P10 Input Right or Button A (0=Pressed) (Read Only)
     */
    public int peekFF00() {
        int status = cpu.mem.RAM[0xFF00];

        int directionCombined = (keyState[DOWN] << 3) | (keyState[UP] << 2) | (keyState[LEFT] << 1) | (keyState[RIGHT]);
        int buttonsCombined = (keyState[START] << 3) | (keyState[SELECT] << 2) | (keyState[B] << 1) | (keyState[A]);

        directionCombined = (~directionCombined) & 0x0f;
        buttonsCombined = (~buttonsCombined) & 0x0f;

        int controlFlags = cpu.mem.RAM[0xFF00] & 0x30;

        if ((status & 0x10) > 0)
            return ((buttonsCombined) & 0x0F) | controlFlags;
        if ((status & 0x20) > 0)
            return ((directionCombined) & 0x0F) | controlFlags;

        return 0;
    }
//	public static int UP = 4;
//	public static int DOWN = 5;
//	public static int LEFT = 6;
//	public static int RIGHT = 7;

}
