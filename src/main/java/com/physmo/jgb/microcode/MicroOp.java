package com.physmo.jgb.microcode;

public enum MicroOp {
    STOP,
    TODO,
    NOP,
    HALT,
    PREFIX_CB,

    // Increment and decrement
    INC_8,      // INC temp (8 bit)
    INC_16,     // INC temp (16 bit)
    DEC_8,      // DEC temp (8 bit)
    DEC_16,     // DEC temp (16 bit)

    LD_8, LD_16,
    ADD_8, ADD_16,
    SUB_8, SUB_16,

    FETCH_A,     // Read register B to temp
    FETCH_B,     // Read register B to temp
    FETCH_C,     // Read register B to temp
    FETCH_D,     // Read register B to temp
    FETCH_E,     // Read register B to temp
    FETCH_H,     // Read register B to temp
    FETCH_L,     // Read register B to temp
    FETCH_F,     // Read register B to temp
    FETCH_I,     // Read register B to temp
    FETCH_R,     // Read register B to temp
    FETCH_8,     // Read next byte
    FETCH_16,    // Read next word
    FETCH_AF,    // Read register AF to temp
    FETCH_BC,    // Read register BC to temp
    FETCH_DE,    // Read register DE to temp
    FETCH_HL,    // Read register HL to temp
    FETCH_IX,    // Read register IX to temp
    FETCH_IY,    // Read register IX to temp
    FETCH_SP,    // Read Stack Pointer to temp
    FETCH_PC,    // Read Program Counter to temp
    FETCH_pBC,   // Read value in memory location BC to temp
    FETCH_16_ADDRESS,
    FETCH_pHL,
    FETCH_BYTE_FROM_ADDR,

    STORE_A,    // Store temp in A
    STORE_B,    // Store temp in B
    STORE_C,    // Store temp in B
    STORE_D,    // Store temp in B
    STORE_E,    // Store temp in B
    STORE_H,    // Store temp in B
    STORE_L,    // Store temp in B
    STORE_BC,
    //STORE_pBC, // DON'T LIKE THIS
    STORE_p16WORD,
    STORE_HL,
    STORE_DE,
    STORE_SP,

    RLCA,

    ADD_HL,

    SET_ADDR_FROM_HL,

    SET_ADDR_FROM_BC,
    SET_ADDR_FROM_DE,
    STORE_BYTE_AT_ADDRESS,
    RRCA,
    SET_ADDR_FROM_HL_INC,
    SET_ADDR_FROM_HL_DEC,


    RLA,
    RL,

    RRA,
    DAA,
    CPL,

    SCF,
    CCF,

    ADD,
    ADC,
    SUB,
    SBC,
    CP,
    OR,
    XOR,
    AND,

    RET, RETI, RETZ, RETNZ,
    RETNC, RETC, PUSHW,
    POPW,

    CALLNZ,

    // JUMPS
    JP, JRNZ, JRZ,
    JRNC, JRC, JR,
    JPZ, JPNZ, JPC, JPNC,

    // CALL
    CALLZ, CALLC, CALLNC, CALL,

    // Reset vectors
    RST_00H, RST_08H,
    RST_10H, RST_18H,
    RST_20H, RST_28H,
    RST_30H, RST_38H,


    LDZPGA, ADDSPNN,
    FETCH_ZPG,
    STORE_AF,
    // Interrupt enable/disable
    DI, EI,
    LDHLSPN,
}


// 0x04     inc b   READ_B, INC_8, STORE_B