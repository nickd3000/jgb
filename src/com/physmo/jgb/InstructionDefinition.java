package com.physmo.jgb;

/*
 	Minimal set of address modes:
 	
 	(BC)
	(DE)
	(HL)
	(nnnn)
	
	A
	B
	C
	D
	E
	H
	L
	Z
	BC
	DE
	NZ
	disp
	HL
	NC
	nn
	nnnn
	SP

 */
enum ADDRMODE {
	NONE, __BC, __DE, __HL, __nnnn, A, B, C, D, E, H, L, Z, AF, BC, DE, NZ, disp, HL, NC, nn, nnnn, SP,
	// A, ABS, ABS_X, ABS_Y, IMDT, IMPL, IND, X_IND, IND_Y, REL, ZPG, ZPG_X, ZPG_Y,
};

enum COMMAND {
	NOP, LD, INC, XOR, LDD, LDI, PREFIX, JRNZ, LDZPGCA, LDZPGNNA,LDAZPGNN, CALL, PUSHW, POPW, RLA, DEC, DECW, JP, DI, EI
	, INCW, RET, CP, OR, AND, CPL,JRZ,JR,SUB,ADD,HALT,
	RST_38H, RST_8H, RETZ, RETNZ, JRNC, JRC, ADC, SBC, JPZ, RRCA, ADDHL, RETNC, CALLNZ, RETI, JPNZ, LDHLSPN, RST_28H, RLCA, JPNC, CALLZ, SCF, CALLC, RST_00H, RR, JPC, RST_20H, RETC, RST_18H, DAA, RST_30H, RST_10H, CCF, LDAZPGC, ADDSPNN, CALLNC, LDSPHL, LDW,
};

public enum InstructionDefinition {

	NOP(0x00, COMMAND.NOP, 1, ADDRMODE.NONE, ADDRMODE.NONE),

	HALT(0x76, COMMAND.HALT, 1, ADDRMODE.NONE, ADDRMODE.NONE), // 76    HALT
	
	LD_BC_nnnn(0x01, COMMAND.LD, 3, ADDRMODE.BC, ADDRMODE.nnnn), 	// 01    LD   BC,nnnn
	LD_pBC_A(0x02, COMMAND.LD, 1, ADDRMODE.__BC, ADDRMODE.A), 		// 02 LD (BC),A
	
	LD_HL_nnnn(0x21, COMMAND.LD, 3, ADDRMODE.HL, ADDRMODE.nnnn), // 21 LD HL,nnnn
	
	LD_HL_SPnn(0xF8, COMMAND.LDHLSPN, 2, ADDRMODE.HL, ADDRMODE.nn), // F8    LD   HL,SP+dd     ---- special (old ret m) (nocash corrected)
	
	DI(0xF3, COMMAND.DI, 1, ADDRMODE.NONE, ADDRMODE.NONE), // F3    DI - disable interrupts?
	EI(0xFB, COMMAND.EI, 1, ADDRMODE.NONE, ADDRMODE.NONE), // FB    EI - enable interrupts?

	RRCA(0x0F, COMMAND.RRCA, 1, ADDRMODE.A, ADDRMODE.NONE), // 0F    RRCA
	
	RR_A(0x1F, COMMAND.RR, 1, ADDRMODE.A, ADDRMODE.NONE), 	// 1F    RRA
	
	RLCA(0x07, COMMAND.RLCA, 1, ADDRMODE.A, ADDRMODE.NONE), //07    RLCA
	
	INC_pHL(0x34, COMMAND.INC, 1, ADDRMODE.__HL, ADDRMODE.NONE), // 34    INC  (HL)
	
	INC_A(0x3C, COMMAND.INC, 1, ADDRMODE.A, ADDRMODE.NONE), // 3C INC A
	INC_B(0x04, COMMAND.INC, 1, ADDRMODE.B, ADDRMODE.NONE), // 04 INC B
	INC_C(0x0C, COMMAND.INC, 1, ADDRMODE.C, ADDRMODE.NONE), // 0C INC C
	INC_D(0x14, COMMAND.INC, 1, ADDRMODE.D, ADDRMODE.NONE), // 14 INC D
	INC_E(0x1C, COMMAND.INC, 1, ADDRMODE.E, ADDRMODE.NONE), // 1C INC E
	INC_H(0x24, COMMAND.INC, 1, ADDRMODE.H, ADDRMODE.NONE), // 24 INC H
	INC_L(0x2C, COMMAND.INC, 1, ADDRMODE.L, ADDRMODE.NONE), // 2C INC L
	
	INC_BC(0x03, COMMAND.INCW, 1, ADDRMODE.BC, ADDRMODE.NONE), // 03 INC BC
	INC_HL(0x23, COMMAND.INCW, 1, ADDRMODE.HL, ADDRMODE.NONE), 	//23    INC  HL
	INC_DE(0x13, COMMAND.INCW, 1, ADDRMODE.DE, ADDRMODE.NONE), 	//13    INC  DE
	INC_SP(0x33, COMMAND.INCW, 1, ADDRMODE.SP, ADDRMODE.NONE), 	//33    INC  SP
	
	DEC_A(0x3D, COMMAND.DEC, 1, ADDRMODE.A, ADDRMODE.NONE), //	3D    DEC  A
	DEC_B(0x05, COMMAND.DEC, 1, ADDRMODE.B, ADDRMODE.NONE), //	05    DEC  B
	DEC_C(0x0D, COMMAND.DEC, 1, ADDRMODE.C, ADDRMODE.NONE), //	0D    DEC  C
	DEC_D(0x15, COMMAND.DEC, 1, ADDRMODE.D, ADDRMODE.NONE), //	15    DEC  D
	DEC_E(0x1D, COMMAND.DEC, 1, ADDRMODE.E, ADDRMODE.NONE), //	1D    DEC  E
	DEC_H(0x25, COMMAND.DEC, 1, ADDRMODE.H, ADDRMODE.NONE), //	25    DEC  H
	DEC_L(0x2D, COMMAND.DEC, 1, ADDRMODE.L, ADDRMODE.NONE), //	2D    DEC  L
	DEC_pHL(0x35, COMMAND.DEC, 1, ADDRMODE.__HL, ADDRMODE.NONE), //	35    DEC  (HL)
	DEC_HL(0x2B, COMMAND.DECW, 1, ADDRMODE.HL, ADDRMODE.NONE), //	2B    DEC  HL
	DEC_SP(0x3B, COMMAND.DECW, 1, ADDRMODE.SP, ADDRMODE.NONE), //	3B    DEC  SP
	DEC_BC(0x0B, COMMAND.DECW, 1, ADDRMODE.BC, ADDRMODE.NONE), //	0B    DEC  BC
	DEC_DE(0x1B, COMMAND.DECW, 1, ADDRMODE.DE, ADDRMODE.NONE), //	1B    DEC  DE

	LD_SP_nnnn(0x31, COMMAND.LD, 3, ADDRMODE.SP, ADDRMODE.nnnn), // 31 LD SP,nnnn
	LD_DE_nnnn(0x11, COMMAND.LD, 3, ADDRMODE.DE, ADDRMODE.nnnn), // 11    LD   DE,nnnn
	LD_SP_HL(0xF9, COMMAND.LDSPHL, 3, ADDRMODE.SP, ADDRMODE.HL), // F9    LD   SP,HL

	DAA(0x27, COMMAND.DAA, 1, ADDRMODE.SP, ADDRMODE.HL), // 27    DAA
	
	XOR_B(0xA8, COMMAND.XOR, 1, ADDRMODE.B, ADDRMODE.NONE), //	A8    XOR  B
	XOR_C(0xA9, COMMAND.XOR, 1, ADDRMODE.C, ADDRMODE.NONE), //	A9    XOR  C
	XOR_D(0xAA, COMMAND.XOR, 1, ADDRMODE.D, ADDRMODE.NONE), //	AA    XOR  D
	XOR_E(0xAB, COMMAND.XOR, 1, ADDRMODE.E, ADDRMODE.NONE), //	AB    XOR  E
	XOR_H(0xAC, COMMAND.XOR, 1, ADDRMODE.H, ADDRMODE.NONE), //	AC    XOR  H
	XOR_L(0xAD, COMMAND.XOR, 1, ADDRMODE.L, ADDRMODE.NONE), //	AD    XOR  L
	XOR_pHL(0xAE, COMMAND.XOR, 1, ADDRMODE.__HL, ADDRMODE.NONE), //	AE    XOR  (HL)
	XOR_A(0xAF, COMMAND.XOR, 1, ADDRMODE.A, ADDRMODE.NONE), // AF XOR A
	
	XOR_nn(0xEE, COMMAND.XOR, 2, ADDRMODE.nn, ADDRMODE.NONE), // EE    XOR  nn
	
	LDD_pHL_A(0x32, COMMAND.LDD, 1, ADDRMODE.__HL, ADDRMODE.A), // 32 LDD (HL),A ---- special (old remapped ld (nnnn),a)
	LDI_pHL_A(0x22, COMMAND.LDI, 1, ADDRMODE.__HL, ADDRMODE.A), // 22    LDI  (HL),A       ---- special (old ld (nnnn),hl)
	
	LDD_A_pHL(0x3A, COMMAND.LDD, 1, ADDRMODE.A, ADDRMODE.__HL), // 3A    LDD  A,(HL) 
	LDI_A_pHL(0x2A, COMMAND.LDI, 1, ADDRMODE.A, ADDRMODE.__HL), // 2A    LDI  A,(HL)       ---- special (old ld hl,(nnnn))
	
	PREFIX(0xCB, COMMAND.PREFIX, 2, ADDRMODE.nn, ADDRMODE.NONE), // CB nn ---(see beyond)---
	JRNZ(0x20, COMMAND.JRNZ, 2, ADDRMODE.nn, ADDRMODE.NONE), // 20    JR   NZ,disp
	RET(0xC9, COMMAND.RET, 1, ADDRMODE.NONE, ADDRMODE.NONE), //C9    RET
	
	RETZ(0xC8, COMMAND.RETZ, 1, ADDRMODE.NONE, ADDRMODE.NONE), // C8    RET  Z
	RETNZ(0xC0, COMMAND.RETNZ, 1, ADDRMODE.NONE, ADDRMODE.NONE), // C0    RET  NZ
	
	RETI(0xD9, COMMAND.RETI, 1, ADDRMODE.NONE, ADDRMODE.NONE), // D9    RETI              ---- remapped (old exx)
	
	RETNC(0xD0, COMMAND.RETNC, 1, ADDRMODE.NONE, ADDRMODE.NONE), // D0    RET  NC
	RETC(0xD8, COMMAND.RETC, 1, ADDRMODE.NONE, ADDRMODE.NONE), // D8    RET  C
	
	
	JP_nnnn(0xC3, COMMAND.JP, 3, ADDRMODE.nnnn, ADDRMODE.NONE),	// C3    JP   nnnn
	JPNC(0xD2, COMMAND.JPNC, 3, ADDRMODE.nnnn, ADDRMODE.NONE),	// D2    JP   NC,nnnn
	JPC(0xDA, COMMAND.JPC, 3, ADDRMODE.nnnn, ADDRMODE.NONE),	// DA    JP   C,nnnn
	JP_HL(0xE9, COMMAND.JP, 1, ADDRMODE.HL, ADDRMODE.NONE),	// E9    JP   (HL)
	
	CALLNC(0xD4, COMMAND.CALLNC, 3, ADDRMODE.nnnn, ADDRMODE.NONE),	// D4    CALL NC,nnnn
	
	JPZ(0xCA, COMMAND.JPZ, 3, ADDRMODE.nnnn, ADDRMODE.NONE),	// CA    JP   Z,nnnn
	JPNZ(0xC2, COMMAND.JPNZ, 3, ADDRMODE.nnnn, ADDRMODE.NONE),	// C2    JP   NZ,nnnn
	
	JRZ(0x28, COMMAND.JRZ, 2, ADDRMODE.nn, ADDRMODE.NONE), // 28    JR   Z,disp
	
	JR(0x18, COMMAND.JR, 2, ADDRMODE.nn, ADDRMODE.NONE), // 18    JR   disp

	JRNC(0x30, COMMAND.JRNC, 2, ADDRMODE.nn, ADDRMODE.NONE), // 30    JR   NC,disp
	
	JRC(0x38, COMMAND.JRC, 2, ADDRMODE.nn, ADDRMODE.NONE), // 38    JR   C,disp
	
	
	
	LD_A_nn(0x3E, COMMAND.LD, 2, ADDRMODE.A, ADDRMODE.nn), // 3E LD A,nn
	LD_B_nn(0x06, COMMAND.LD, 2, ADDRMODE.B, ADDRMODE.nn), // 06 LD B,nn
	LD_C_nn(0x0E, COMMAND.LD, 2, ADDRMODE.C, ADDRMODE.nn), // 0E LD C,nn
	LD_D_nn(0x16, COMMAND.LD, 2, ADDRMODE.D, ADDRMODE.nn), // 16 LD D,nn
	LD_E_nn(0x1E, COMMAND.LD, 2, ADDRMODE.E, ADDRMODE.nn), // 1E LD E,nn
	LD_H_nn(0x26, COMMAND.LD, 2, ADDRMODE.H, ADDRMODE.nn), // 26 LD H,nn
	LD_L_nn(0x2E, COMMAND.LD, 2, ADDRMODE.L, ADDRMODE.nn), // 2E LD L,nn

	LD_pHL_A(0x77, COMMAND.LD, 1, ADDRMODE.__HL, ADDRMODE.A), // 77 LD (HL),A
	LD_pHL_B(0x70, COMMAND.LD, 1, ADDRMODE.__HL, ADDRMODE.B), // 70 LD (HL),B
	LD_pHL_C(0x71, COMMAND.LD, 1, ADDRMODE.__HL, ADDRMODE.C), // 71 LD (HL),C
	LD_pHL_D(0x72, COMMAND.LD, 1, ADDRMODE.__HL, ADDRMODE.D), // 72 LD (HL),D
	LD_pHL_E(0x73, COMMAND.LD, 1, ADDRMODE.__HL, ADDRMODE.E), // 73 LD (HL),E
	LD_pHL_H(0x74, COMMAND.LD, 1, ADDRMODE.__HL, ADDRMODE.H), // 74 LD (HL),H
	LD_pHL_L(0x75, COMMAND.LD, 1, ADDRMODE.__HL, ADDRMODE.L), // 75 LD (HL),L

	LD_pHL_nn(0x36, COMMAND.LD, 2, ADDRMODE.__HL, ADDRMODE.nn), //36    LD   (HL),nn
	

	
	LD_A_pDE(0x1A, COMMAND.LD, 1, ADDRMODE.A, ADDRMODE.__DE), 	//	1A    LD   A,(DE)
	LD_A_pBC(0x0A, COMMAND.LD, 1, ADDRMODE.A, ADDRMODE.__BC), 	//	0A    LD   A,(BC)
	LD_A_pHL(0x7E, COMMAND.LD, 1, ADDRMODE.A, ADDRMODE.__HL), 	//	7E    LD   A,(HL)
	
	LD_pDE_A(0x12, COMMAND.LD, 1, ADDRMODE.__DE, ADDRMODE.A), 	// 12    LD   (DE),A
	
	LD_ZPG_C_A(0xE2, COMMAND.LDZPGCA, 1, ADDRMODE.NONE, ADDRMODE.NONE), // E2 LD ($FF00+C),A ---- special (old jp
																		// po,nnnn)

	LD_ZPG_nn_A(0xE0, COMMAND.LDZPGNNA, 2, ADDRMODE.nn, ADDRMODE.NONE), // E0 LD ($FF00+nn),A ---- special (old ret po)

	LD_A_ZPG_nn(0xF0, COMMAND.LDAZPGNN, 2, ADDRMODE.nn, ADDRMODE.NONE), // F0    LD   A,($FF00+nn) ---- special (old ret p)
	
	LD_A_ZPG_C(0xF2, COMMAND.LDAZPGC, 2, ADDRMODE.NONE, ADDRMODE.NONE), // F2    LD   A,(C)        ---- special (old jp p,nnnn)
	
	CALL_nnnn(0xCD, COMMAND.CALL, 3, ADDRMODE.nnnn, ADDRMODE.NONE), // CD    CALL nnnn	
	CALLNZ_nnnn(0xC4, COMMAND.CALLNZ, 3, ADDRMODE.nnnn, ADDRMODE.NONE), // C4    CALL NZ,nnnn
	CALLZ_nnnn(0xCC, COMMAND.CALLZ, 3, ADDRMODE.nnnn, ADDRMODE.NONE), 	// CC    CALL Z,nnnn
	
	CALLC_nnnn(0xDC, COMMAND.CALLC, 3, ADDRMODE.nnnn, ADDRMODE.NONE), 		// DC    CALL C,nnnn
	
	LD_pnnnn_A(0xEA, COMMAND.LD, 3, ADDRMODE.__nnnn, ADDRMODE.A), // EA    LD   (nnnn),A     ---- special (old jp pe,nnnn)
	LD_pnnnn_SP(0x08, COMMAND.LDW, 3, ADDRMODE.__nnnn, ADDRMODE.SP), // 08    LD   (nnnn),SP    ---- special (old ex af,af)
	
	LD_B_B(0x40, COMMAND.LD, 1, ADDRMODE.B, ADDRMODE.B), //	40    LD   B,B                          
	LD_B_C(0x41, COMMAND.LD, 1, ADDRMODE.B, ADDRMODE.C), //	41    LD   B,C                          
	LD_B_D(0x42, COMMAND.LD, 1, ADDRMODE.B, ADDRMODE.D), //	42    LD   B,D                          
	LD_B_E(0x43, COMMAND.LD, 1, ADDRMODE.B, ADDRMODE.E), //	43    LD   B,E                          
	LD_B_H(0x44, COMMAND.LD, 1, ADDRMODE.B, ADDRMODE.H), //	44    LD   B,H                          
	LD_B_L(0x45, COMMAND.LD, 1, ADDRMODE.B, ADDRMODE.L), //	45    LD   B,L      
	LD_B_pHL(0x46, COMMAND.LD, 1, ADDRMODE.B, ADDRMODE.__HL), // 46    LD   B,(HL)
	LD_B_A(0x47, COMMAND.LD, 1, ADDRMODE.B, ADDRMODE.A), //	47    LD   B,A
	//
	LD_C_B(0x48, COMMAND.LD, 1, ADDRMODE.C, ADDRMODE.B), //	48    LD   C,B                          
	LD_C_C(0x49, COMMAND.LD, 1, ADDRMODE.C, ADDRMODE.C), //	49    LD   C,C                          
	LD_C_D(0x4A, COMMAND.LD, 1, ADDRMODE.C, ADDRMODE.D), //	4A    LD   C,D                          
	LD_C_E(0x4B, COMMAND.LD, 1, ADDRMODE.C, ADDRMODE.E), //	4B    LD   C,E                          
	LD_C_H(0x4C, COMMAND.LD, 1, ADDRMODE.C, ADDRMODE.H), //	4C    LD   C,H                          
	LD_C_L(0x4D, COMMAND.LD, 1, ADDRMODE.C, ADDRMODE.L), //	4D    LD   C,L       
	LD_C_pHL(0x4E, COMMAND.LD, 1, ADDRMODE.C, ADDRMODE.__HL), // 4E    LD   C,(HL)
	LD_C_A(0x4F, COMMAND.LD, 1, ADDRMODE.C, ADDRMODE.A), //	4F    LD   C,A  
	//
	LD_D_B(0x50, COMMAND.LD, 1, ADDRMODE.D, ADDRMODE.B), //	50    LD   D,B                          
	LD_D_C(0x51, COMMAND.LD, 1, ADDRMODE.D, ADDRMODE.C), //	51    LD   D,C                          
	LD_D_D(0x52, COMMAND.LD, 1, ADDRMODE.D, ADDRMODE.D), //	52    LD   D,D                          
	LD_D_E(0x53, COMMAND.LD, 1, ADDRMODE.D, ADDRMODE.E), //	53    LD   D,E                          
	LD_D_H(0x54, COMMAND.LD, 1, ADDRMODE.D, ADDRMODE.H), //	54    LD   D,H                          
	LD_D_L(0x55, COMMAND.LD, 1, ADDRMODE.D, ADDRMODE.L), //	55    LD   D,L     
	LD_D_pHL(0x56, COMMAND.LD, 1, ADDRMODE.D, ADDRMODE.__HL), // 56    LD   D,(HL)
	LD_D_A(0x57, COMMAND.LD, 1, ADDRMODE.D, ADDRMODE.A), //	57    LD   D,A   
	//
	LD_E_B(0x58, COMMAND.LD, 1, ADDRMODE.E, ADDRMODE.B), //	58    LD   E,B                           
	LD_E_C(0x59, COMMAND.LD, 1, ADDRMODE.E, ADDRMODE.C), //	59    LD   E,C                          
	LD_E_D(0x5A, COMMAND.LD, 1, ADDRMODE.E, ADDRMODE.D), //	5A    LD   E,D                          
	LD_E_E(0x5B, COMMAND.LD, 1, ADDRMODE.E, ADDRMODE.E), //	5B    LD   E,E                          
	LD_E_H(0x5C, COMMAND.LD, 1, ADDRMODE.E, ADDRMODE.H), //	5C    LD   E,H                          
	LD_E_L(0x5D, COMMAND.LD, 1, ADDRMODE.E, ADDRMODE.L), //	5D    LD   E,L   
	LD_E_pHL(0x5E, COMMAND.LD, 1, ADDRMODE.E, ADDRMODE.__HL), // 5E    LD   E,(HL)
	LD_E_A(0x5F, COMMAND.LD, 1, ADDRMODE.E, ADDRMODE.A), //	5F    LD   E,A  
	//
	LD_H_B(0x60, COMMAND.LD, 1, ADDRMODE.H, ADDRMODE.B), //	60    LD   H,B
	LD_H_C(0x61, COMMAND.LD, 1, ADDRMODE.H, ADDRMODE.C), //	61    LD   H,C
	LD_H_D(0x62, COMMAND.LD, 1, ADDRMODE.H, ADDRMODE.D), //	62    LD   H,D
	LD_H_E(0x63, COMMAND.LD, 1, ADDRMODE.H, ADDRMODE.E), //	63    LD   H,E
	LD_H_H(0x64, COMMAND.LD, 1, ADDRMODE.H, ADDRMODE.H), //	64    LD   H,H
	LD_H_L(0x65, COMMAND.LD, 1, ADDRMODE.H, ADDRMODE.L), //	65    LD   H,L
	LD_H_pHL(0x66, COMMAND.LD, 1, ADDRMODE.H, ADDRMODE.__HL), // 66    LD   H,(HL)
	LD_H_A(0x67, COMMAND.LD, 1, ADDRMODE.H, ADDRMODE.A), //	67    LD   H,A
	//
	LD_L_B(0x68, COMMAND.LD, 1, ADDRMODE.L, ADDRMODE.B), //	68    LD   L,B
	LD_L_C(0x69, COMMAND.LD, 1, ADDRMODE.L, ADDRMODE.C), //	69    LD   L,C
	LD_L_D(0x6A, COMMAND.LD, 1, ADDRMODE.L, ADDRMODE.D), //	6A    LD   L,D
	LD_L_E(0x6B, COMMAND.LD, 1, ADDRMODE.L, ADDRMODE.E), //	6B    LD   L,E
	LD_L_H(0x6C, COMMAND.LD, 1, ADDRMODE.L, ADDRMODE.H), //	6C    LD   L,H
	LD_L_L(0x6D, COMMAND.LD, 1, ADDRMODE.L, ADDRMODE.L), //	6D    LD   L,L
	LD_L_pHL(0x6E, COMMAND.LD, 1, ADDRMODE.L, ADDRMODE.__HL), // // 6E    LD   L,(HL)
	LD_L_A(0x6F, COMMAND.LD, 1, ADDRMODE.L, ADDRMODE.A), //	6F    LD   L,A
	//
	LD_A_B(0x78, COMMAND.LD, 1, ADDRMODE.A, ADDRMODE.B), //	78    LD   A,B
	LD_A_C(0x79, COMMAND.LD, 1, ADDRMODE.A, ADDRMODE.C), //	79    LD   A,C
	LD_A_D(0x7A, COMMAND.LD, 1, ADDRMODE.A, ADDRMODE.D), //	7A    LD   A,D
	LD_A_E(0x7B, COMMAND.LD, 1, ADDRMODE.A, ADDRMODE.E), //	7B    LD   A,E
	LD_A_H(0x7C, COMMAND.LD, 1, ADDRMODE.A, ADDRMODE.H), //	7C    LD   A,H
	LD_A_L(0x7D, COMMAND.LD, 1, ADDRMODE.A, ADDRMODE.L), //	7D    LD   A,L
	LD_A_A(0x7F, COMMAND.LD, 1, ADDRMODE.A, ADDRMODE.A), //	7F    LD   A,A
	
	LD_A_pNNNN(0xFA, COMMAND.LD, 3, ADDRMODE.A, ADDRMODE.__nnnn), // FA    LD   A,(nnnn)     ---- special (old jp m,nnnn)
	
	PUSHW_AF(0xF5, COMMAND.PUSHW, 1, ADDRMODE.AF, ADDRMODE.NONE), //	F5    PUSH AF
	PUSHW_BC(0xC5, COMMAND.PUSHW, 1, ADDRMODE.BC, ADDRMODE.NONE), //	C5    PUSH BC
	PUSHW_DE(0xD5, COMMAND.PUSHW, 1, ADDRMODE.DE, ADDRMODE.NONE), //	D5    PUSH DE
	PUSHW_HL(0xE5, COMMAND.PUSHW, 1, ADDRMODE.HL, ADDRMODE.NONE), //	E5    PUSH HL
	//
	POPW_HL(0xE1, COMMAND.POPW, 1, ADDRMODE.HL, ADDRMODE.NONE), //	E1    POP  HL
	POPW_BC(0xC1, COMMAND.POPW, 1, ADDRMODE.BC, ADDRMODE.NONE), //	C1    POP  BC
	POPW_DE(0xD1, COMMAND.POPW, 1, ADDRMODE.DE, ADDRMODE.NONE), //	D1    POP  DE
	POPW_AF(0xF1, COMMAND.POPW, 1, ADDRMODE.AF, ADDRMODE.NONE), //	F1    POP  AF

	OR_B(0xB0, COMMAND.OR, 1, ADDRMODE.B, ADDRMODE.NONE), // B0    OR   B
	OR_C(0xB1, COMMAND.OR, 1, ADDRMODE.C, ADDRMODE.NONE), // B1    OR   C
	OR_D(0xB2, COMMAND.OR, 1, ADDRMODE.D, ADDRMODE.NONE), 	// B2    OR   D
	OR_E(0xB3, COMMAND.OR, 1, ADDRMODE.E, ADDRMODE.NONE), 	// B3    OR   E
	OR_H(0xB4, COMMAND.OR, 1, ADDRMODE.H, ADDRMODE.NONE), 	// B4    OR   H
	OR_L(0xB5, COMMAND.OR, 1, ADDRMODE.L, ADDRMODE.NONE), 	// B5    OR   L
	OR_pHL(0xB6, COMMAND.OR, 1, ADDRMODE.__HL, ADDRMODE.NONE), 	// B6    OR   (HL)
	OR_A(0xB7, COMMAND.OR, 1, ADDRMODE.A, ADDRMODE.NONE), 	// B7    OR   A
	
	OR_nn(0xF6, COMMAND.OR, 2, ADDRMODE.nn, ADDRMODE.NONE), 	// F6    OR   nn
	
	AND_B(0xA0, COMMAND.AND, 1, ADDRMODE.B, ADDRMODE.NONE), // A0    AND  B
	AND_C(0xA1, COMMAND.AND, 1, ADDRMODE.C, ADDRMODE.NONE), // A1    AND  C
	AND_D(0xA2, COMMAND.AND, 1, ADDRMODE.D, ADDRMODE.NONE), // A2    AND  D
	AND_E(0xA3, COMMAND.AND, 1, ADDRMODE.E, ADDRMODE.NONE), 	// A3    AND  E
	AND_H(0xA4, COMMAND.AND, 1, ADDRMODE.H, ADDRMODE.NONE), 	// A4    AND  H
	AND_L(0xA5, COMMAND.AND, 1, ADDRMODE.L, ADDRMODE.NONE), 	// A5    AND  L
	AND_pHL(0xA6, COMMAND.AND, 1, ADDRMODE.__HL, ADDRMODE.NONE), 	// A6    AND  (HL)
	AND_A(0xA7, COMMAND.AND, 1, ADDRMODE.A, ADDRMODE.NONE), 	// A7    AND  A

	AND_nn(0xE6, COMMAND.AND, 2, ADDRMODE.nn, ADDRMODE.NONE), 	// E6    AND  nn
	
	RLA(0x17, COMMAND.RLA, 1, ADDRMODE.NONE, ADDRMODE.NONE),	// 17    RLA
	
	CP_nn(0xFE, COMMAND.CP, 2, ADDRMODE.nn, ADDRMODE.NONE),	// FE    CP   nn
	
	RST_00H(0xC7, COMMAND.RST_00H, 1, ADDRMODE.NONE, ADDRMODE.NONE),	// C7    RST  00H
	RST_8H(0xCF, COMMAND.RST_8H, 1, ADDRMODE.NONE, ADDRMODE.NONE),		// CF    RST  8
	RST_10H(0xD7, COMMAND.RST_10H, 1, ADDRMODE.NONE, ADDRMODE.NONE),	// D7    RST  10H
	RST_20H(0xE7, COMMAND.RST_20H, 1, ADDRMODE.NONE, ADDRMODE.NONE),	// E7    RST  20H
	RST_28H(0xEF, COMMAND.RST_28H, 1, ADDRMODE.NONE, ADDRMODE.NONE),	// EF    RST  28H
	RST_38H(0xFF, COMMAND.RST_38H, 1, ADDRMODE.NONE, ADDRMODE.NONE),	// FF    RST  38H
	RST_18H(0xDF, COMMAND.RST_18H, 1, ADDRMODE.NONE, ADDRMODE.NONE),	// 	DF    RST  18H
	RST_30H(0xF7, COMMAND.RST_30H, 1, ADDRMODE.NONE, ADDRMODE.NONE),	// F7    RST  30H
	
	
	CPL(0x2F, COMMAND.CPL, 1, ADDRMODE.NONE, ADDRMODE.NONE),	// 2F    CPL (Flip all bits in A)
	SUB_B(0x90, COMMAND.SUB, 1, ADDRMODE.B, ADDRMODE.NONE), // 90    SUB  B 
	SUB_C(0x91, COMMAND.SUB, 1, ADDRMODE.C, ADDRMODE.NONE), // 91    SUB  C 
	SUB_D(0x92, COMMAND.SUB, 1, ADDRMODE.D, ADDRMODE.NONE), // 92    SUB  D 
	SUB_E(0x93, COMMAND.SUB, 1, ADDRMODE.E, ADDRMODE.NONE), // 93    SUB  E 
	SUB_H(0x94, COMMAND.SUB, 1, ADDRMODE.H, ADDRMODE.NONE), // 94    SUB  H 
	SUB_L(0x95, COMMAND.SUB, 1, ADDRMODE.L, ADDRMODE.NONE), // 95    SUB  L 
	SUB_pHL(0x96, COMMAND.SUB, 1, ADDRMODE.__HL, ADDRMODE.NONE), // 96    SUB  (HL) 
	SUB_A(0x97, COMMAND.SUB, 1, ADDRMODE.A, ADDRMODE.NONE), // 97    SUB  A      
	
	SUB_nn(0xD6, COMMAND.SUB, 2, ADDRMODE.nn, ADDRMODE.NONE), 	// D6    SUB  nn
	
	ADD_B(0x80, COMMAND.ADD, 1, ADDRMODE.B, ADDRMODE.NONE), // 80    ADD  A,B 
	ADD_C(0x81, COMMAND.ADD, 1, ADDRMODE.C, ADDRMODE.NONE), // 81    ADD  A,C 
	ADD_D(0x82, COMMAND.ADD, 1, ADDRMODE.D, ADDRMODE.NONE), // 82    ADD  A,D 
	ADD_E(0x83, COMMAND.ADD, 1, ADDRMODE.E, ADDRMODE.NONE), 	// 83    ADD  A,E 
	ADD_H(0x84, COMMAND.ADD, 1, ADDRMODE.H, ADDRMODE.NONE), 	// 84    ADD  A,H 
	ADD_L(0x85, COMMAND.ADD, 1, ADDRMODE.L, ADDRMODE.NONE), 	// 85    ADD  A,L 
	ADD_pHL(0x86, COMMAND.ADD, 1, ADDRMODE.__HL, ADDRMODE.NONE), 	// 86    ADD  A,(HL)
	ADD_A(0x87, COMMAND.ADD, 1, ADDRMODE.A, ADDRMODE.NONE), 	// 87    ADD  A,A   
	
	ADD_nn(0xC6, COMMAND.ADD, 2, ADDRMODE.nn, ADDRMODE.NONE), 	// C6    ADD  A,nn
	
	ADD_HL_DE(0x19, COMMAND.ADDHL, 1, ADDRMODE.HL, ADDRMODE.DE), 	// 19    ADD  HL,DE
	ADD_HL_BC(0x09, COMMAND.ADDHL, 1, ADDRMODE.HL, ADDRMODE.BC), 	// 09    ADD  HL,BC
	ADD_HL_HL(0x29, COMMAND.ADDHL, 1, ADDRMODE.HL, ADDRMODE.HL), 	// 29    ADD  HL,HL
	ADD_HL_SP(0x39, COMMAND.ADDHL, 1, ADDRMODE.HL, ADDRMODE.SP), 	// 39    ADD  HL,SP
	
	ADD_SP_nn(0xE8, COMMAND.ADDSPNN, 2, ADDRMODE.nn, ADDRMODE.NONE), 	// E8    ADD  SP,dd        ---- special (old ret pe) (nocash extended as shortint)
	
	CP_B(0xB8, COMMAND.CP, 1, ADDRMODE.B, ADDRMODE.NONE), 	//	B8    CP   B
	CP_C(0xB9, COMMAND.CP, 1, ADDRMODE.C, ADDRMODE.NONE),//	B9    CP   C
	CP_D(0xBA, COMMAND.CP, 1, ADDRMODE.D, ADDRMODE.NONE),//	BA    CP   D
	CP_E(0xBB, COMMAND.CP, 1, ADDRMODE.E, ADDRMODE.NONE),//	BB    CP   E
	CP_H(0xBC, COMMAND.CP, 1, ADDRMODE.H, ADDRMODE.NONE),//	BC    CP   H
	CP_L(0xBD, COMMAND.CP, 1, ADDRMODE.L, ADDRMODE.NONE),//	BD    CP   L
	CP_pHL(0xBE, COMMAND.CP, 1, ADDRMODE.__HL, ADDRMODE.NONE),//	BE    CP   (HL)
	CP_A(0xBF, COMMAND.CP, 1, ADDRMODE.A, ADDRMODE.NONE),//	BF    CP   A
	
	ADC_B(0x88, COMMAND.ADC, 1, ADDRMODE.B, ADDRMODE.NONE),//	88    ADC  A,B 
	ADC_C(0x89, COMMAND.ADC, 1, ADDRMODE.C, ADDRMODE.NONE),//	89    ADC  A,C 
	ADC_D(0x8A, COMMAND.ADC, 1, ADDRMODE.D, ADDRMODE.NONE),//	8A    ADC  A,D 
	ADC_E(0x8B, COMMAND.ADC, 1, ADDRMODE.E, ADDRMODE.NONE),//	8B    ADC  A,E 
	ADC_H(0x8C, COMMAND.ADC, 1, ADDRMODE.H, ADDRMODE.NONE),//	8C    ADC  A,H 
	ADC_L(0x8D, COMMAND.ADC, 1, ADDRMODE.L, ADDRMODE.NONE),//	8D    ADC  A,L 
	ADC_pHL(0x8E, COMMAND.ADC, 1, ADDRMODE.__HL, ADDRMODE.NONE),//	8E    ADC  A,(HL)
	ADC_A(0x8F, COMMAND.ADC, 1, ADDRMODE.A, ADDRMODE.NONE),//	8F    ADC  A,A   
	
	ADC_nn(0xCE, COMMAND.ADC, 2, ADDRMODE.nn, ADDRMODE.NONE), // CE    ADC  A,nn
	
	SBC_B(0x98, COMMAND.SBC, 1, ADDRMODE.B, ADDRMODE.NONE),//	98    SBC  A,B 
	SBC_C(0x99, COMMAND.SBC, 1, ADDRMODE.C, ADDRMODE.NONE),//	99    SBC  A,C 
	SBC_D(0x9A, COMMAND.SBC, 1, ADDRMODE.D, ADDRMODE.NONE),//	9A    SBC  A,D 
	SBC_E(0x9B, COMMAND.SBC, 1, ADDRMODE.E, ADDRMODE.NONE),//	9B    SBC  A,E 
	SBC_H(0x9C, COMMAND.SBC, 1, ADDRMODE.H, ADDRMODE.NONE),//	9C    SBC  A,H 
	SBC_L(0x9D, COMMAND.SBC, 1, ADDRMODE.L, ADDRMODE.NONE),//	9D    SBC  A,L 
	SBC_pHL(0x9E, COMMAND.SBC, 1, ADDRMODE.__HL, ADDRMODE.NONE),//	9E    SBC  A,(HL)
	SBC_A(0x9F, COMMAND.SBC, 1, ADDRMODE.A, ADDRMODE.NONE),//	9F    SBC  A,A  
	   
	SBC_nn(0xDE, COMMAND.SBC, 2, ADDRMODE.nn, ADDRMODE.NONE),//DE    SBC  A,nn     (nocash added, this opcode does existed, e.g. used by kwirk)
	
	SCF(0x37, COMMAND.SCF, 1, ADDRMODE.NONE, ADDRMODE.NONE),	// 37    SCF
	CCF(0x3F, COMMAND.CCF, 1, ADDRMODE.NONE, ADDRMODE.NONE),	// 3F    CCF
	
	;;;;
	
	
	private int opcode;
	private COMMAND command;
	private int numBytes;
	private ADDRMODE am1;
	private ADDRMODE am2;

	// register, cycles
	InstructionDefinition(int opcode, COMMAND name, int numBytes, ADDRMODE am1, ADDRMODE am2) {
		if (opcode==0xDB) {
			System.out.println("contruscted DB");
		}
		this.opcode = opcode;
		this.command = name;
		this.numBytes = numBytes;
		this.am1 = am1;
		this.am2 = am2;
	}

	public int getOpCode() {
		return opcode;
	}

	public COMMAND getCommand() {
		return command;
	}

	public ADDRMODE getAddressMode1() {
		return am1;
	}

	public ADDRMODE getAddressMode2() {
		return am2;
	}

	public String getDescription() {
		String str = " " + command.toString() + "  " + am1.toString();
		return str;
	}

	public int getNumBytes() {
		return numBytes;
	}
	
	public static InstructionDefinition getEnumFromId(int id) {
		
		for (InstructionDefinition ir : InstructionDefinition.values()) {
			if (id == ir.opcode) {
				return ir;
			}
		}

		return null;
	}

}
