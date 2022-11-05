package com.physmo.jgb.microcode;


import java.util.HashMap;
import java.util.Map;

import static com.physmo.jgb.microcode.MicroOp.*;


public class Microcode {

    MicroOp[][] n = new MicroOp[500][];
    Map<Integer, String> names = new HashMap<>();

    public Microcode() {
        for (int i = 0; i < n.length; i++) {
            n[i] = new MicroOp[]{TODO};
        }

        define(0x00, "NOP", NOP);
        define(0x01, "LD BC,d16", FETCH_16, STORE_BC);
        define(0x02, "LD (BC),A", FETCH_A, SET_ADDR_FROM_BC, STORE_BYTE_AT_ADDRESS);
        define(0x03, "INC BC", FETCH_BC, INC_16, STORE_BC);
        define(0x04, "INC B", FETCH_B, INC_8, STORE_B);
        define(0x05, "DEC B", FETCH_B, DEC_8, STORE_B);
        define(0x06, "LD B,d8", FETCH_8, STORE_B);
        define(0x07, "RLCA", RLCA); // 8-bit rotation left
        define(0x08, "LD (a16),SP", FETCH_SP, FETCH_16_ADDRESS, STORE_p16WORD);

        define(0x09, "ADD HL,BC", FETCH_BC, ADD_HL, STORE_HL);
        define(0x0A, "LD A,(BC)", SET_ADDR_FROM_BC, FETCH_BYTE_FROM_ADDR, STORE_A);
        define(0x0B, "DEC BC", FETCH_BC, DEC_16, STORE_BC);
        define(0x0C, "INC C", FETCH_C, INC_8, STORE_C);
        define(0x0D, "DEC C", FETCH_C, DEC_8, STORE_C);
        define(0x0E, "LD C,d8", FETCH_8, STORE_C);
        define(0x0F, "RRCA", RRCA);

        define(0x10, "STOP", STOP);

        define(0x11, "LD DE,d16", FETCH_16, STORE_DE);
        define(0x12, "LD (DE),A", FETCH_A, SET_ADDR_FROM_DE, STORE_BYTE_AT_ADDRESS);
        define(0x13, "INC DE", FETCH_DE, INC_16, STORE_DE);
        define(0x14, "INC D", FETCH_D, INC_8, STORE_D);
        define(0x15, "DEC D", FETCH_D, DEC_8, STORE_D);
        define(0x16, "LD D,d8", FETCH_8, STORE_D);
        define(0x17, "RLA", FETCH_A, RL, STORE_A);
        define(0x18, "JR r8", FETCH_8, JR);
        define(0x19, "ADD HL,DE", FETCH_DE, ADD_HL, STORE_HL);
        define(0x1A, "LD A,(DE)", SET_ADDR_FROM_DE, FETCH_BYTE_FROM_ADDR, STORE_A);
        define(0x1B, "DEC DE", FETCH_DE, DEC_16, STORE_DE);
        define(0x1C, "INC E", FETCH_E, INC_8, STORE_E);
        define(0x1D, "DEC E", FETCH_E, DEC_8, STORE_E);
        define(0x1E, "LD E,d8", FETCH_8, STORE_E);
        define(0x1F, "RRA", RRA);

        define(0x20, "JR NZ,r8", FETCH_8, JRNZ);
        define(0x21, "LD HL,d16", FETCH_16, STORE_HL);
        define(0x22, "LD (HL+),A", FETCH_A, SET_ADDR_FROM_HL_INC, STORE_BYTE_AT_ADDRESS);
        define(0x23, "INC HL", FETCH_HL, INC_16, STORE_HL);
        define(0x24, "INC H", FETCH_H, INC_8, STORE_H);
        define(0x25, "DEC H", FETCH_H, DEC_8, STORE_H);
        define(0x26, "LD H,d8", FETCH_8, STORE_H);
        define(0x27, "DAA", DAA);
        define(0x28, "JR Z,r8", FETCH_8, JRZ);
        define(0x29, "ADD HL,HL", FETCH_HL, ADD_HL, STORE_HL);
        define(0x2A, "LD A,(HL)", SET_ADDR_FROM_HL_INC, FETCH_BYTE_FROM_ADDR, STORE_A);
        define(0x2B, "DEC HL", FETCH_HL, DEC_16, STORE_HL);
        define(0x2C, "INC L", FETCH_L, INC_8, STORE_L);
        define(0x2D, "DEC L", FETCH_L, DEC_8, STORE_L);
        define(0x2E, "LD L,d8", FETCH_8, STORE_L);
        define(0x2F, "CPL", CPL);

        define(0x30, "JR NC,r8", FETCH_8, JRNC);
        define(0x31, "LD SP,d16", FETCH_16, STORE_SP);
        define(0x32, "LD (HL-),A", FETCH_A, SET_ADDR_FROM_HL_DEC, STORE_BYTE_AT_ADDRESS);
        define(0x33, "INC SP", FETCH_SP, INC_16, STORE_SP);
        define(0x34, "INC (HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, INC_8, STORE_BYTE_AT_ADDRESS);
        define(0x35, "DEC (HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, DEC_8, STORE_BYTE_AT_ADDRESS);
        define(0x36, "LD (HL),d8", FETCH_8, SET_ADDR_FROM_HL, STORE_BYTE_AT_ADDRESS);
        define(0x37, "SCF", SCF);
        define(0x38, "JR C,r8", FETCH_8, JRC);
        define(0x39, "ADD HL,SP", FETCH_SP, ADD_HL, STORE_HL);
        define(0x3A, "LD A,(HL-)", SET_ADDR_FROM_HL_DEC, FETCH_BYTE_FROM_ADDR, STORE_A);
        define(0x3B, "DEC SP", FETCH_SP, DEC_16, STORE_SP);
        define(0x3C, "INC A", FETCH_A, INC_8, STORE_A);
        define(0x3D, "DEC A", FETCH_A, DEC_8, STORE_A);
        define(0x3E, "LD A,d8", FETCH_8, STORE_A);
        define(0x3F, "CCF", CCF);


        define(0x40, "LD B,B", FETCH_B, STORE_B);
        define(0x41, "LD B,C", FETCH_C, STORE_B);
        define(0x42, "LD B,D", FETCH_D, STORE_B);
        define(0x43, "LD B,E", FETCH_E, STORE_B);
        define(0x44, "LD B,H", FETCH_H, STORE_B);
        define(0x45, "LD B,L", FETCH_L, STORE_B);
        define(0x46, "LD B,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, STORE_B);
        define(0x47, "LD B,A", FETCH_A, STORE_B);
        define(0x48, "LD C,B", FETCH_B, STORE_C);
        define(0x49, "LD C,C", FETCH_C, STORE_C);
        define(0x4A, "LD C,D", FETCH_D, STORE_C);
        define(0x4B, "LD C,E", FETCH_E, STORE_C);
        define(0x4C, "LD C,H", FETCH_H, STORE_C);
        define(0x4D, "LD C,L", FETCH_L, STORE_C);
        define(0x4E, "LD C,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, STORE_C);
        define(0x4F, "LD C,A", FETCH_A, STORE_C);

        define(0x50, "LD D,B", FETCH_B, STORE_D);
        define(0x51, "LD D,C", FETCH_C, STORE_D);
        define(0x52, "LD D,D", FETCH_D, STORE_D);
        define(0x53, "LD D,E", FETCH_E, STORE_D);
        define(0x54, "LD D,H", FETCH_H, STORE_D);
        define(0x55, "LD D,L", FETCH_L, STORE_D);
        define(0x56, "LD D,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, STORE_D);
        define(0x57, "LD D,A", FETCH_A, STORE_D);
        define(0x58, "LD E,B", FETCH_B, STORE_E);
        define(0x59, "LD E,C", FETCH_C, STORE_E);
        define(0x5A, "LD E,D", FETCH_D, STORE_E);
        define(0x5B, "LD E,E", FETCH_E, STORE_E);
        define(0x5C, "LD E,H", FETCH_H, STORE_E);
        define(0x5D, "LD E,L", FETCH_L, STORE_E);
        define(0x5E, "LD E,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, STORE_E);
        define(0x5F, "LD E,A", FETCH_A, STORE_E);

        define(0x60, "LD H,B", FETCH_B, STORE_H);
        define(0x61, "LD H,C", FETCH_C, STORE_H);
        define(0x62, "LD H,D", FETCH_D, STORE_H);
        define(0x63, "LD H,E", FETCH_E, STORE_H);
        define(0x64, "LD H,H", FETCH_H, STORE_H);
        define(0x65, "LD H,L", FETCH_L, STORE_H);
        define(0x66, "LD H,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, STORE_H);
        define(0x67, "LD H,A", FETCH_A, STORE_H);
        define(0x68, "LD L,B", FETCH_B, STORE_L);
        define(0x69, "LD L,C", FETCH_C, STORE_L);
        define(0x6A, "LD L,D", FETCH_D, STORE_L);
        define(0x6B, "LD L,E", FETCH_E, STORE_L);
        define(0x6C, "LD L,H", FETCH_H, STORE_L);
        define(0x6D, "LD L,L", FETCH_L, STORE_L);
        define(0x6E, "LD L,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, STORE_L);
        define(0x6F, "LD L,A", FETCH_A, STORE_L);

        define(0x70, "LD (HL),B", SET_ADDR_FROM_HL, FETCH_B, STORE_BYTE_AT_ADDRESS);
        define(0x71, "LD (HL),C", SET_ADDR_FROM_HL, FETCH_C, STORE_BYTE_AT_ADDRESS);
        define(0x72, "LD (HL),D", SET_ADDR_FROM_HL, FETCH_D, STORE_BYTE_AT_ADDRESS);
        define(0x73, "LD (HL),E", SET_ADDR_FROM_HL, FETCH_E, STORE_BYTE_AT_ADDRESS);
        define(0x74, "LD (HL),H", SET_ADDR_FROM_HL, FETCH_H, STORE_BYTE_AT_ADDRESS);
        define(0x75, "LD (HL),L", SET_ADDR_FROM_HL, FETCH_L, STORE_BYTE_AT_ADDRESS);
        define(0x76, "HALT", HALT);
        define(0x77, "LD (HL),A", SET_ADDR_FROM_HL, FETCH_A, STORE_BYTE_AT_ADDRESS);
        define(0x78, "LD A,B", FETCH_B, STORE_A);
        define(0x79, "LD A,C", FETCH_C, STORE_A);
        define(0x7A, "LD A,D", FETCH_D, STORE_A);
        define(0x7B, "LD A,E", FETCH_E, STORE_A);
        define(0x7C, "LD A,H", FETCH_H, STORE_A);
        define(0x7D, "LD A,L", FETCH_L, STORE_A);
        define(0x7E, "LD A,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, STORE_A);
        define(0x7F, "LD A,A", FETCH_A, STORE_A);

        define(0x80, "ADD A,B", FETCH_B, ADD);
        define(0x81, "ADD A,C", FETCH_C, ADD);
        define(0x82, "ADD A,D", FETCH_D, ADD);
        define(0x83, "ADD A,E", FETCH_E, ADD);
        define(0x84, "ADD A,H", FETCH_H, ADD);
        define(0x85, "ADD A,L", FETCH_L, ADD);
        define(0x86, "ADD A,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, ADD);
        define(0x87, "ADD A,A", FETCH_A, ADD);
        define(0x88, "ADC A,B", FETCH_B, ADC);
        define(0x89, "ADC A,C", FETCH_C, ADC);
        define(0x8A, "ADC A,D", FETCH_D, ADC);
        define(0x8B, "ADC A,E", FETCH_E, ADC);
        define(0x8C, "ADC A,H", FETCH_H, ADC);
        define(0x8D, "ADC A,L", FETCH_L, ADC);
        define(0x8E, "ADC A,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, ADC);
        define(0x8F, "ADC A,A", FETCH_A, ADC);

        define(0x90, "SUB A,B", FETCH_B, SUB);
        define(0x91, "SUB A,C", FETCH_C, SUB);
        define(0x92, "SUB A,D", FETCH_D, SUB);
        define(0x93, "SUB A,E", FETCH_E, SUB);
        define(0x94, "SUB A,H", FETCH_H, SUB);
        define(0x95, "SUB A,L", FETCH_L, SUB);
        define(0x96, "SUB A,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, SUB);
        define(0x97, "SUB A,A", FETCH_A, SUB);
        define(0x98, "SBC A,B", FETCH_B, SBC);
        define(0x99, "SBC A,C", FETCH_C, SBC);
        define(0x9A, "SBC A,D", FETCH_D, SBC);
        define(0x9B, "SBC A,E", FETCH_E, SBC);
        define(0x9C, "SBC A,H", FETCH_H, SBC);
        define(0x9D, "SBC A,L", FETCH_L, SBC);
        define(0x9E, "SBC A,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, SBC);
        define(0x9F, "SBC A,A", FETCH_A, SBC);


        define(0xA0, "AND A,B", FETCH_B, AND);
        define(0xA1, "AND A,C", FETCH_C, AND);
        define(0xA2, "AND A,D", FETCH_D, AND);
        define(0xA3, "AND A,E", FETCH_E, AND);
        define(0xA4, "AND A,H", FETCH_H, AND);
        define(0xA5, "AND A,L", FETCH_L, AND);
        define(0xA6, "AND A,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, AND);
        define(0xA7, "AND A,A", FETCH_A, AND);
        define(0xA8, "XOR A,B", FETCH_B, XOR);
        define(0xA9, "XOR A,C", FETCH_C, XOR);
        define(0xAA, "XOR A,D", FETCH_D, XOR);
        define(0xAB, "XOR A,E", FETCH_E, XOR);
        define(0xAC, "XOR A,H", FETCH_H, XOR);
        define(0xAD, "XOR A,L", FETCH_L, XOR);
        define(0xAE, "XOR A,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, XOR);
        define(0xAF, "XOR A,A", FETCH_A, XOR);

        define(0xB0, "OR A,B", FETCH_B, OR);
        define(0xB1, "OR A,C", FETCH_C, OR);
        define(0xB2, "OR A,D", FETCH_D, OR);
        define(0xB3, "OR A,E", FETCH_E, OR);
        define(0xB4, "OR A,H", FETCH_H, OR);
        define(0xB5, "OR A,L", FETCH_L, OR);
        define(0xB6, "OR A,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, OR);
        define(0xB7, "OR A,A", FETCH_A, OR);
        define(0xB8, "CP A,B", FETCH_B, CP);
        define(0xB9, "CP A,C", FETCH_C, CP);
        define(0xBA, "CP A,D", FETCH_D, CP);
        define(0xBB, "CP A,E", FETCH_E, CP);
        define(0xBC, "CP A,H", FETCH_H, CP);
        define(0xBD, "CP A,L", FETCH_L, CP);
        define(0xBE, "CP A,(HL)", SET_ADDR_FROM_HL, FETCH_BYTE_FROM_ADDR, CP);
        define(0xBF, "CP A,A", FETCH_A, CP);

        define(0xC0, "RET NZ", RETNZ);
        define(0xC1, "POP BC", POPW, STORE_BC);
        define(0xC2, "JP NZ,a16", FETCH_16_ADDRESS, JPNZ);
        define(0xC3, "JP a16", FETCH_16_ADDRESS, JP);
        define(0xC4, "CALL NZ,a16", FETCH_16_ADDRESS, CALLNZ);
        define(0xC5, "PUSH BC", FETCH_BC, PUSHW);
        define(0xC6, "ADD A,d8", FETCH_8, ADD);
        define(0xC7, "RST 00H", RST_00H);
        define(0xC8, "RET Z", RETZ);
        define(0xC9, "RET", RET);
        define(0xCA, "JP Z,a16", FETCH_16_ADDRESS, JPZ);
        define(0xCB, "PREFIX CB", FETCH_8, PREFIX_CB);
        define(0xCC, "CALL Z,a16", FETCH_16_ADDRESS, CALLZ);
        define(0xCD, "CALL a16", FETCH_16_ADDRESS, CALL);
        define(0xCE, "ADC A,d8", FETCH_8, ADC);
        define(0xCF, "RST 08H", RST_08H);

        define(0xD0, "RET NC", RETNC);
        define(0xD1, "POP DE", POPW, STORE_DE);
        define(0xD2, "JP NC,a16", FETCH_16_ADDRESS, JPNC);
        define(0xD3, "NO OP", NOP);
        define(0xD4, "CALL NC,a16", FETCH_16_ADDRESS, CALLNC);
        define(0xD5, "PUSH DE", FETCH_DE, PUSHW);
        define(0xD6, "SUB d8", FETCH_8, SUB);
        define(0xD7, "RST 10H", RST_10H);
        define(0xD8, "RET C", RETC);
        define(0xD9, "RETI", RETI);
        define(0xDA, "JP C,a16", FETCH_16_ADDRESS, JPC);
        define(0xDB, "NO OP", NOP);
        define(0xDC, "CALL C,a16", FETCH_16_ADDRESS, CALLC);
        define(0xDD, "NO OP", NOP);
        define(0xDE, "SBC A,d8", FETCH_8, SBC);
        define(0xDF, "RST 18H", RST_18H);

        define(0xE0, "LDH (a8),A", FETCH_8, LDZPGA);
        define(0xE1, "POP HL", POPW, STORE_HL);
        define(0xE2, "LD (C),A", FETCH_C, LDZPGA);
        define(0xE3, "NO OP", NOP);
        define(0xE4, "NO OP", NOP);
        define(0xE5, "PUSH HL", FETCH_HL, PUSHW);
        define(0xE6, "AND d8", FETCH_8, AND);
        define(0xE7, "RST 20H", RST_20H);
        define(0xE8, "ADD SP,r8", FETCH_8, ADDSPNN);
        define(0xE9, "JP (HL)", SET_ADDR_FROM_HL, JP);
        define(0xEA, "LD (a16),A", FETCH_16_ADDRESS, FETCH_A, STORE_BYTE_AT_ADDRESS);
        define(0xEB, "NO OP", NOP);
        define(0xEC, "NO OP", NOP);
        define(0xED, "NO OP", NOP);
        define(0xEE, "XOR d8", FETCH_8, XOR);
        define(0xEF, "RST 28H", RST_28H);

        define(0xF0, "LDH A,(a8)", FETCH_8, FETCH_ZPG, STORE_A);
        define(0xF1, "POP AF", POPW, STORE_AF);
        define(0xF2, "LD A,(C)", FETCH_C, FETCH_ZPG, STORE_A);
        define(0xF3, "DI", DI);
        define(0xF4, "NO OP", NOP);
        define(0xF5, "PUSH AF", FETCH_AF, PUSHW);
        define(0xF6, "OR d8", FETCH_8, OR);
        define(0xF7, "RST 30H", RST_30H);
        define(0xF8, "LD HL,SP+r8", FETCH_8, LDHLSPN);
        define(0xF9, "LD SP,HL", FETCH_HL, STORE_SP);
        define(0xFA, "LD A,(a16)", FETCH_16_ADDRESS, FETCH_BYTE_FROM_ADDR, STORE_A);
        define(0xFB, "EI", EI);
        define(0xFC, "NO OP", NOP);
        define(0xFD, "NO OP", NOP);
        define(0xFE, "CP d8", FETCH_8, CP);
        define(0xFF, "RST 38H", RST_38H);
    }

    public void define(int opCode, String name, MicroOp... microcodes) {
        n[opCode] = microcodes;
        names.put(opCode, name);
    }

    public MicroOp[] getInstructionCode(int instruction) {
        return n[instruction];
    }

    public String getInstructionName(int instruction) {
        return names.get(instruction);
    }

}
