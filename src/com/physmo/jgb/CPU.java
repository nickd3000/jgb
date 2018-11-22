package com.physmo.jgb;

class AddressContainer {
	public int oprnd = 0, val = 0, addr = 0;
	public String bytesRead = "";
	ADDRMODE mode = ADDRMODE.NONE;
}

public class CPU {

	public static int INT_VBLANK = 1; //   Vblank off	Vblank on
	public static int INT_LCDSTAT = 1<<1; //	LCD stat off	LCD stat on
	public static int INT_TIMER = 1<<2; //	Timer off	Timer on
	public static int INT_SERIAL = 1<<3; //	Serial off	Serial on
	public static int INT_JOYPAD = 	1<<4; //	Joypad off
	
	public static boolean displayInstruction = false;

	MEM mem = null;

	// Registers.
	int A,B,C,D,E,H,L;
	int PC; // Program counter
	int SP=0xFFFE; // Stack pointer
	int FL;
	int cycles = 0;
	int tickCount = 0;
	int interruptEnabled = 1;
	
	int fakeVerticalBlank = 0;
	
	public void attachHardware(MEM mem) {
		this.mem = mem;
	}

	public void tick() {
		tickCount++;
		if (tickCount>30000000) displayInstruction=true;
		
		// Handle interrupts before checking current instruction.
		checkInterrupts();
		
		int entryPC = PC;
		int currentInstruction = mem.peek(PC++);

		//
//		fakeVerticalBlank++;
//		mem.RAM[0xFF44]=(fakeVerticalBlank/12)&0xff;
		
		
		
		// TODO: Don't create these objects every time, make them static and just clear
		// them.
		AddressContainer ac1 = new AddressContainer();
		AddressContainer ac2 = new AddressContainer();

		// Get this instructions definition.
		InstructionDefinition def = InstructionDefinition.getEnumFromId(currentInstruction);

		if (def==null) {
			System.out.println("No InstructionDefinition at "+Utils.toHex4(entryPC)+" : "+Utils.toHex2(currentInstruction) + "   ");
		}
		
		COMMAND command = def.getCommand();
		ADDRMODE addrmode1 = def.getAddressMode1();
		ADDRMODE addrmode2 = def.getAddressMode2();

		processAddressMode(ac1, addrmode1);
		processAddressMode(ac2, addrmode2);

		if (displayInstruction) {
			String dis = Debug.dissasemble(this, entryPC);
			dis = Utils.padToLength(dis, 24);
			dis += Debug.getFlags(this);
			dis += Debug.getRegisters(this);
			dis += Debug.getStackData(this, 5);
			dis += Debug.getInterruptFlags(this);
			System.out.println(dis);
		}
		
		int wrk = 0;
		
		switch (command) {
		case NOP:
			break;
		case RET:
			wrk = popW();
			PC = wrk;
			break;
		case RETI:
			wrk = popW();
			PC = wrk;
			enableInterrupts();
			break;	
		case RETZ:
			if (testFlag(FLAG_ZERO)) {
				wrk = popW();
				PC = wrk;
			}
			break;
		case RETNZ:
			if (!testFlag(FLAG_ZERO)) {
				wrk = popW();
				PC = wrk;
			}
			break;
		case RETNC:
			if (!testFlag(FLAG_CARRY)) {
				wrk = popW();
				PC = wrk;
			}
			break;
		case JP:
			PC = ac1.val;
			break;
			
		case DI:
			disableInterrupts();
			break;
		case EI:
			enableInterrupts();
			break;
		case INC:
			wrk = ac1.val + 1;
			if (wrk>0xff) wrk=0;
			mem.poke(ac1.addr, wrk);
			handleZeroFlag(wrk);
			unsetFlag(FLAG_ADDSUB);
			break;
		case INCW:
			wrk = ac1.val+1;
			if (wrk>0xffff) wrk-=0xffff;
			mem.poke(ac1.addr, wrk);
			handleZeroFlag(wrk);
			unsetFlag(FLAG_ADDSUB);
			break;
		case DEC:
			wrk = ac1.val-1;
			if (wrk==-1) wrk=0xff;
			//if (val<0) val+=0xff;
			mem.poke(ac1.addr, wrk);
			handleZeroFlag(wrk);
			setFlag(FLAG_ADDSUB);
			
			break;
		case DECW:
			wrk = ac1.val-1;
			if (wrk<0) wrk+=0xffff+1;
			//if (val<0) val+=0xff;
			mem.poke(ac1.addr, wrk);

			// NO FLAGS AFFECTED
			
			break;
		case LD:
			wrk = ac2.val;
			mem.poke(ac1.addr, ac2.val);
			handleZeroFlag(wrk);
			unsetFlag(FLAG_ADDSUB);
			
			break;
		case LDD: // Load and decrement?
			wrk = ac2.val;
			mem.poke(ac1.addr, ac2.val);
			// how we do this - this insruction is meant to change the pointer too...
			//mem.poke(ac1.addr, ac1.val + 1);
			
			if (def.getAddressMode1() == ADDRMODE.__HL) {
				//System.out.println("get HL:"+getHL()+"  0x"+Utils.toHex4(getHL()));
				setHL(getHL()-1);
			}
			
			handleZeroFlag(wrk);
			unsetFlag(FLAG_ADDSUB);
			
			break;
		case LDI: // Load and decrement?
			wrk=ac2.val;
			mem.poke(ac1.addr, ac2.val);

			if (def.getAddressMode1() == ADDRMODE.__HL) {
				setHL(getHL()+1);
			}
			if (def.getAddressMode2() == ADDRMODE.__HL) {
				setHL(getHL()+1);
			}
			//handleZeroFlag(wrk);
			//unsetFlag(FLAG_ADDSUB);
			break;
		case XOR:
			wrk = A ^ ac1.val;
			A = wrk&0xff;
			
			handleZeroFlag(wrk);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_CARRY);
			unsetFlag(FLAG_HALFCARRY);
			
			break;
		case OR:
			wrk = A | ac1.val;
			A = wrk & 0xff;
			
			handleZeroFlag(wrk);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_CARRY);
			unsetFlag(FLAG_HALFCARRY);
			
			break;
		case AND:
			wrk = A & ac1.val;
			A = wrk;
			
			handleZeroFlag(wrk);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_CARRY);
			unsetFlag(FLAG_HALFCARRY);
			
			break;
		case SUB:
			wrk = A - ac1.val;
			A = wrk&0xff;
			
			if (wrk<0) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			
			handleZeroFlag(wrk);
			
			
			setFlag(FLAG_ADDSUB);
			
			unsetFlag(FLAG_HALFCARRY);
			
			break;
		case ADD:
			wrk = A + ac1.val;
			A = wrk&0xff;
			
			if (wrk>0xff) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			handleZeroFlag(wrk);
			
			unsetFlag(FLAG_ADDSUB);
			
			unsetFlag(FLAG_HALFCARRY);
			
			break;
		case ADDHL:
			wrk = getHL() + ac2.val;
			
			setHL(wrk&0xffff);
			unsetFlag(FLAG_ADDSUB);
			
			break;
		case ADC:
			wrk = A + ac1.val + (testFlag(FLAG_CARRY)?1:0);
			A = wrk&0xff;
			
			handleZeroFlag(wrk);
			
			if (wrk>0xff) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			
			unsetFlag(FLAG_ADDSUB);
			
			unsetFlag(FLAG_HALFCARRY);
			
			break;
		case SBC:
			wrk = A - ac1.val - (testFlag(FLAG_CARRY)?1:0);
			A = wrk&0xff;
			
			if (wrk>0xff) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			if (wrk==0) setFlag(FLAG_ZERO);
			else unsetFlag(FLAG_ZERO);
			unsetFlag(FLAG_ADDSUB);
			
			unsetFlag(FLAG_HALFCARRY);
			
			break;
		case PREFIX:
				CPUPrefix.processPrefixCommand(this, ac1.val);
			break;
		case SCF:
			setFlag(FLAG_CARRY);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_HALFCARRY);
			break;
		case JRNZ:
			if (testFlag(FLAG_ZERO)==false) {
				jumpRelative(ac1.val);
//				int tc = ac1.val&0xff;
//				tc = convertSignedByte(tc);
//				PC=PC+tc;
			}
			break;
		case JRZ:
			if (testFlag(FLAG_ZERO)==true) {
				jumpRelative(ac1.val);
//				int tc = ac1.val;
//				tc = convertSignedByte(tc);
//				System.out.println("Helper: "+ac1.val+"   tc:"+tc);
//				PC=PC+tc;
			}
			break;
		case JRNC:
			if (testFlag(FLAG_CARRY)==false) {
				jumpRelative(ac1.val);
//				int tc = ac1.val;
//				tc = convertSignedByte(tc);
//				System.out.println("Jump relative: "+ac1.val+"   tc:"+tc);
//				PC=PC+tc;
			}
			break;
		case JRC:
			if (testFlag(FLAG_CARRY)==true) {
				jumpRelative(ac1.val);
//				int tc = ac1.val;
//				
//				tc = convertSignedByte(tc);
//				
//				System.out.println("Jump relative: "+ac1.val+"   tc:"+tc);
//				
//				PC=PC+tc;
			}
			break;
		case JR:
			jumpRelative(ac1.val);
//			
//				int tc = ac1.val;
//				tc = convertSignedByte(tc);
//				System.out.println("Jump relative: "+ac1.val+"   tc:"+tc);
//				PC=PC+tc;
			
			break;
		case JPZ:
			if (testFlag(FLAG_ZERO)==true) {
				wrk = ac1.val;
				PC=wrk;
			}
			break;
		case JPNZ:
			if (testFlag(FLAG_ZERO)==false) {
				wrk = ac1.val;
				PC=wrk;
			}
			break;
		case JPNC:
			if (testFlag(FLAG_CARRY)==false) {
				wrk = ac1.val;
				PC=wrk;
			}
			break;
		case LDZPGCA:
			mem.poke(0xff00+C, A);
			break;
		case LDZPGNNA:
			mem.poke(0xff00+(ac1.val&0xff), A);
			break;
		case LDAZPGNN:
			A = mem.peek(0xff00+ac1.val)&0xff;
			break;
		case LDHLSPN:
			int ptr = SP+convertSignedByte(ac1.val&0xff);
			wrk = combineBytes(mem.RAM[ptr],mem.RAM[ptr+1]);
			setHL(wrk);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_ZERO);
			break;
		case RRCA:
			wrk = ac1.val;
			if (testFlag(FLAG_CARRY)) wrk+=0x100;

			if ((wrk&1)>0) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			
			wrk = wrk>>1;
			mem.poke(ac1.addr, wrk&0xff);
			
			break;
		case RLCA:
			wrk = ac1.val;

			wrk = (wrk<<1)+(testFlag(FLAG_CARRY)?1:0);
			if (wrk>0xff) setFlag(FLAG_CARRY);
			else  unsetFlag(FLAG_CARRY);
			
			mem.poke(ac1.addr, wrk&0xff);
			break;
		case CALL:
			//pushW(entryPC+1);
			pushW(PC);
			PC = ac1.val;
			break;
		case CALLNZ:
			//pushW(entryPC+1);
			if (testFlag(FLAG_ZERO)==false) {
				pushW(PC);
				PC = ac1.val;
			}
			break;
		case CALLZ:
			if (testFlag(FLAG_ZERO)==true) {
				pushW(PC);
				PC = ac1.val;
			}
			break;
		case CALLC:
			if (testFlag(FLAG_CARRY)==true) {
				pushW(PC);
				PC = ac1.val;
			}
			break;
		case PUSHW:
			pushW(ac1.val);
			
			break;
		case POPW:
			wrk = popW();

			mem.poke(ac1.addr, wrk);
			break;
		case RLA:
			A = A << 1;
			if (testFlag(FLAG_CARRY)) A|=1;
			if (A>0xff) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			A = A&0xff;
			break;
		case CP:
			wrk = A - ac1.val;
			
			handleZeroFlag(wrk);
			
			setFlag(FLAG_ADDSUB);
			if (A < ac1.val) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			
			break;
		case CPL:
			wrk = (~A)&0xff;
			
			setFlag(FLAG_ADDSUB);
			setFlag(FLAG_HALFCARRY);
			
			A = wrk;
			break;
		case RST_38H:
			processInterrupt(0x0038);
			break;
		case RST_8H:
			processInterrupt(0x0008);
			break;
		case RST_28H:
			processInterrupt(0x0028);
			break;
		default:
			System.out.println("Unhandled instruction at "+Utils.toHex4(entryPC)+" : "+Utils.toHex2(currentInstruction));
			mem.poke(0xffff+10, 1);
			break;
		}

	}
//int A,B,C,D,E,H,L;
	public static final int ADDR_INVALID = -10; // Operand address that shouldn't be written to.
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
	

	public void checkInterrupts() {
		if (interruptEnabled==0) return;
		
		int intEnabled = mem.RAM[0xFFFF];
		int intFlags = mem.RAM[0xFF0F];
		int masked = intEnabled&intFlags; 
		
		if ((masked&INT_VBLANK)>0) {
			System.out.println("Detected interrupt VBLANK");
			System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			processInterrupt(0x0040); // VBLANK handler.
		}
		
	}
	
	public void processInterrupt(int addr) {
		disableInterrupts();
		pushW(PC);
		PC = addr;
	}
	
	
	
	// Based on the address mode, set up data and addresses to be used by the
	// operation.
	public void processAddressMode(AddressContainer ac, ADDRMODE mode) {
		int peeked = 0;

		ac.mode = mode;
		ac.addr = ADDR_INVALID;

		switch (mode) {
		case NONE:
			break;
		case nn:
			peeked = getNextByte();
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			break;
		case nnnn:
			peeked = getNextWord();
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = peeked;
			break;
		case A:
			peeked = A;
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_A;
			break;
		case B:
			peeked = B;
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_B;
			break;
		case C:
			peeked = C;
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_C;
			break;
		case D:
			peeked = D;
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_D;
			break;
		case E:
			peeked = E;
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_E;
			break;
		case H:
			peeked = H;
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_H;
			break;
		case L:
			peeked = L;
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_L;
			break;

		case SP:
			peeked = SP;
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_SP;
			break;
		case HL:
			peeked = getHL();
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_HL;
			break;
		case BC:
			peeked = getBC();
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_BC;
			break;
		case DE:
			peeked = getDE();
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_DE;
			break;
		case AF:
			peeked = getAF();
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = ADDR_AF;
			break;
		case __HL:
			peeked = mem.peek(getHL());
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = getHL();
			System.out.println("pHL="+Utils.toHex2(peeked));
			break;
		case __BC:
			peeked = mem.peek(getBC());
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = getBC();
			break;
		case __DE:
			peeked = mem.peek(getDE());
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = getDE();
			break;
			
		case __nnnn:
			peeked = getNextWord();
			ac.val = mem.peek(peeked);
			//ac.bytesRead += " " + Utils.toHex2(peeked);
			ac.addr = peeked;
			break;
			
			
		default:
			System.out.println("Address mode not recognised in processAddressMode : "+mode.toString());
			break;
		}

	}
	
	public void jumpRelative(int val) {
		int tc = convertSignedByte(val);
		if (displayInstruction) System.out.println("Jump relative by "+tc+" to "+Utils.toHex4(PC+tc));
		PC=PC+tc;
	}
	
	/*
	 *    16bit Hi   Lo   Name/Function
		  AF    A    -    Accumulator & Flags
		  BC    B    C    BC
		  DE    D    E    DE
		  HL    H    L    HL
		  SP    -    -    Stack Pointer
		  PC    -    -    Program Counter/Pointer
	 */
	
	// Combine two bytes into one 16 bit value.
	public int combineBytes(int a, int b) {
		return ((a&0xff)<<8)+(b&0xff);
	}
	public int getHighByte(int val) {
		return (val>>8)&0xff;
	}
	public int getLowByte(int val) {
		return val&0xff;
	}
	
	public int convertSignedByte(int val) {
		if ((val&0b1000_0000)>0) {
			return -1-((~val)&0xff);
		}
		return val;
	}
	
	public int getAF() {
		return combineBytes(A, FL);
	}
	public int getBC() {
		return combineBytes(B, C);
	}
	public int getDE() {
		return combineBytes(D, E);
	}
	public int getHL() {
		return combineBytes(H, L);
	}
	
	public void setHL(int _h, int _l) {
		this.H = _h;
		this.L = _l;
	}
	public void setHL(int val) {
		this.H = getHighByte(val);
		this.L = getLowByte(val);
	}
	public void setBC(int val) {
		this.B = getHighByte(val);
		this.C = getLowByte(val);
	}
	public void setDE(int val) {
		this.D = getHighByte(val);
		this.E = getLowByte(val);
	}
	public void setAF(int val) {
		this.A = getHighByte(val);
		this.FL = getLowByte(val);
	}
	
	public void enableInterrupts() {
		interruptEnabled=1; 
	}
	
	public void disableInterrupts() {
		interruptEnabled=0;
	}
	
	public void handleZeroFlag(int val) {
		if (val==0) setFlag(FLAG_ZERO);
		else unsetFlag(FLAG_ZERO);
	}
	
	// Get word at PC and move PC on.
	public int getNextWord() {
		int oprnd = mem.peek(PC++) & 0xff;
		oprnd = (oprnd) + ((mem.peek(PC++) & 0xff) << 8);
		return oprnd;
	}

	// Get word at PC and move PC on.
	public int getNextByte() {
		int oprnd = mem.peek(PC++) & 0xff;
		return oprnd;
	}

	/*
	 * 
		  Bit  Name  Set Clr  Expl.
		  7    zf    Z   NZ   Zero Flag
		  6    n     -   -    Add/Sub-Flag (BCD)
		  5    h     -   -    Half Carry Flag (BCD)
		  4    cy    C   NC   Carry Flag
		  3-0  -     -   -    Not used (always zero)
	 */
	public static final int FLAG_ZERO = 1<<7;
	public static final int FLAG_ADDSUB = 1<<6;
	public static final int FLAG_HALFCARRY = 1<<5;
	public static final int FLAG_CARRY = 1<<4;
	
	public void setFlag(int flag) {
		FL |= flag;
	}
	public void unsetFlag(int flag) {
		FL &= ~(flag);
	}
	public boolean testFlag(int flag) {
		if ((FL&flag)>0) return true;
		return false;
	}
	
	// STACK
	public void pushW(int val) {
		mem.poke(SP--, getHighByte(val));
		mem.poke(SP--, getLowByte(val));
	}

	public int popW() {
		int lb = mem.peek(++SP);
		int hb = mem.peek(++SP);
		return combineBytes(hb, lb);
	}
}
