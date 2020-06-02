package com.physmo.jgb;

public enum AddrMap {
    ;
    // These constants represent address mapping for registers, for example,
    // peeking or poking to the address -3 will be mapped to the C register.
    public static final int ADDR_INVALID = 0xDEADBEEF; // Operand address that shouldn't be written to.
    public static final int ADDR_A = -1;
    public static final int ADDR_B = -2;
    public static final int ADDR_C = -3;
    public static final int ADDR_D = -4;
    public static final int ADDR_E = -5;
    public static final int ADDR_H = -6;
    public static final int ADDR_L = -7;
    public static final int ADDR_F = -8;
    public static final int ADDR_SP = -10;
    public static final int ADDR_HL = -11;
    public static final int ADDR_BC = -12;
    public static final int ADDR_DE = -13;
    public static final int ADDR_AF = -14;
}
