package com.physmo.jgb;

class AddressContainer {
	public int oprnd = 0, val = 0, addr = 0;
	public String bytesRead = "";
	ADDRMODE mode = ADDRMODE.NONE;
}

public class CPU {

   public static int ADDR_FF01_SERIAL_DATA = 0xFF01;
   public static int ADDR_FF46_DMA_TRANSFER = 0xFF46;// // FF46 (w) DM Transfer & Start Address
   public static int ADDR_FF45_Y_COMPARE = 0xFF45;   // lyc:  0, // $FF45 (r/w) LY Compare
   public static int ADDR_FF44_Y_SCANLINE = 0xFF44;   // 0xFF44
   public static int ADDR_FF41_LCD_STAT = 0xFF41;   // LCD status register FF41
   
	public static int INT_VBLANK = 1; //   Vblank off	Vblank on
	public static int INT_LCDSTAT = 1<<1; //	LCD stat off	LCD stat on
	public static int INT_TIMER = 1<<2; //	Timer off	Timer on
	public static int INT_SERIAL = 1<<3; //	Serial off	Serial on
	public static int INT_JOYPAD = 	1<<4; //	Joypad off
	
	public static boolean displayInstruction = false;

	MEM mem = null;
	GPU gpu = null;
	INPUT input = null;

	// Registers.
	int A,B,C,D,E,H,L;
	int PC; // Program counter
	int SP=0xFFFE; // Stack pointer
	int FL;
	int cycles = 0;
	int tickCount = 0;
	int interruptEnabled = 0;
	int halt=0;
	int pendingEnableInterrupt=0;
	int pendingDisableInterrupt=0;
	int fakeVerticalBlank = 0;
	int topOfSTack=0; // used for debugging.
	
	public void attachHardware(MEM mem, INPUT input, GPU gpu) {
		this.mem = mem;
		this.input = input;
		this.gpu = gpu;
	}

	int serialBit = 0;
	
	public void tick() {
		//mem.poke(0xC000,0x69);
		
		tickCount++;
		//if (tickCount>30000000) displayInstruction=true;
		//if (tickCount>1401788-10000) displayInstruction=true;
		//if (PC==0x001D) displayInstruction=true;
		//if (PC>0x00FF) displayInstruction=true;
		//displayInstruction=true;
		//if (PC==0x0100) displayInstruction=true;
		//if (PC==0x02A0) displayInstruction=true;
		
		int serialBitOld = serialBit;
		if ((mem.peek(0xFF02)&0x10)>0) serialBit = 1;
		else serialBit=0;
		
		if (serialBitOld!= serialBit)
			System.out.println(""+(char)mem.peek(ADDR_FF01_SERIAL_DATA));
		
		// fake a timer interrupt:
//		if (tickCount%10000==0) {
//			mem.RAM[0xFF0F] |= CPU.INT_TIMER;
//		}
		
		Debug.checkRegisters(this);
		
		FL = FL & 0xF0;
//		// Handle interrupts before checking current instruction.
		checkInterrupts();
		
		if (halt==1) return;
		
		int entryPC = PC;
		int currentInstruction = mem.peek(PC++);

		//
//		fakeVerticalBlank++;
//		mem.RAM[0xFF44]=(fakeVerticalBlank/12)&0xff;
//		

		
		// TODO: Don't create these objects every time, make them static and just clear
		// them.
		AddressContainer ac1 = new AddressContainer();
		AddressContainer ac2 = new AddressContainer();

		// Get this instructions definition.
		InstructionDefinition def = InstructionDefinition.getEnumFromId(currentInstruction&0xff);

		if (def==null) {
			System.out.println("No InstructionDefinition at "+Utils.toHex4(entryPC)+" : "+Utils.toHex2(currentInstruction) + "   " + "tick:"+tickCount);
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
		
		// Handle buffered interrupt enable/disable;
		if (pendingDisableInterrupt>0) {
			pendingDisableInterrupt--;
			if (pendingDisableInterrupt==0) {
				interruptEnabled=0;
			}
		}
		if (pendingEnableInterrupt>0) {
			pendingEnableInterrupt--;
			if (pendingEnableInterrupt==0) {
				interruptEnabled=1;
			}
		}
		
		
		switch (command) {
		case NOP:
			break;
		case HALT:
			//PC--;
			if (interruptEnabled==1)
				halt=1;
			else
				PC++;
			
			//enableInterrupts();
			System.out.println("HALT !!!!!!!!!!!!!!!!!!!!!");
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
		case RETC:
			if (testFlag(FLAG_CARRY)) {
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
			
			handleZeroFlag(wrk&0xff);
			unsetFlag(FLAG_ADDSUB);
			
			if ((ac1.val&0xF)+1>0xF) setFlag(FLAG_HALFCARRY);
			else unsetFlag(FLAG_HALFCARRY);
			
			mem.poke(ac1.addr, wrk&0xff);
			
			break;
		case INCW:
			wrk = ac1.val+1;
			if (wrk>0xffff) wrk=0;
			mem.poke(ac1.addr, wrk&0xffff);
			// NO FLAGS AFFECTED
			break;
		case DEC:
			wrk = ac1.val-1;
			if (wrk<0) wrk=0xff;
			
			handleZeroFlag(wrk);
			setFlag(FLAG_ADDSUB);
			if ((ac1.val&0xF)-1<0) setFlag(FLAG_HALFCARRY);
			else unsetFlag(FLAG_HALFCARRY);
			
			mem.poke(ac1.addr, wrk&0xff);
			
			break;
		case DECW:
			wrk = ac1.val-1;
			if (wrk<0) wrk=0xffff;
			mem.poke(ac1.addr, wrk);
			// NO FLAGS AFFECTED
			break;
		case LDSPHL:
			// TODO: this is new
			SP = getHL();
			//SP = ((H&0xff)<<8)|(L&0xff);
			break;
		case LD:
			wrk = ac2.val;
			if (displayInstruction) System.out.println("addr2:"+Utils.toHex4(ac2.addr)+" val2:"+ac2.val);
			mem.poke(ac1.addr, ac2.val);
			// NO FLAGS AFFECTED
			break;
		case LDD: // Load and decrement?
			wrk = ac2.val;
			mem.poke(ac1.addr, ac2.val);
			// how we do this - this insruction is meant to change the pointer too...
			//mem.poke(ac1.addr, ac1.val + 1);
			
			if (def.getAddressMode1() == ADDRMODE.__HL) {
				//System.out.println("get HL:"+getHL()+"  0x"+Utils.toHex4(getHL()));
				setHL(getHL()-1);
			} else  if (def.getAddressMode2() == ADDRMODE.__HL) {
				//System.out.println("get HL:"+getHL()+"  0x"+Utils.toHex4(getHL()));
				setHL(getHL()-1);
			}  else {
				System.out.println("ERROR LDD");
		}
			if (getHL()<0) setHL(0xffff);
			// NO FLAGS AFFECTED
			break;
		case LDI: // Load and decrement?
			wrk=ac2.val;
			mem.poke(ac1.addr, ac2.val);

			if (def.getAddressMode1() == ADDRMODE.__HL) {
				setHL(getHL()+1);
			} else if (def.getAddressMode2() == ADDRMODE.__HL) {
				setHL(getHL()+1);
			} else {
				System.out.println("ERROR LDI");
			}
			
			if (getHL()>0xffff) setHL(0x0);
			// NO FLAGS AFFECTED
			break;
		case XOR:
			wrk = A ^ ac1.val;
			handleZeroFlag(wrk&0xff);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_CARRY);
			unsetFlag(FLAG_HALFCARRY);
			A = wrk&0xff;
			break;
		case OR:
			wrk = A | ac1.val;
			handleZeroFlag(wrk&0xff);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_CARRY);
			unsetFlag(FLAG_HALFCARRY);
			A = wrk & 0xff;
			break;
		case AND:
			wrk = A & ac1.val;
			A = wrk;
			
			handleZeroFlag(wrk);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_CARRY);
			setFlag(FLAG_HALFCARRY);
			if (displayInstruction) System.out.println("and val:"+Utils.toHex2(ac1.val));
			break;
		case SUB:
			wrk = A - ac1.val;
			
			if (ac1.val>A) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			
			handleZeroFlag(wrk&0xff);
			setFlag(FLAG_ADDSUB);
			//unsetFlag(FLAG_HALFCARRY);
			
			if (((A^ac1.val^wrk) & 0x10)>0) setFlag(FLAG_HALFCARRY); 
			else unsetFlag(FLAG_HALFCARRY);
				
			
			A = wrk&0xff;
			
			break;
		case ADD:
			wrk = A + ac1.val;
			A = wrk&0xff;
			
			if (wrk>0xff) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			handleZeroFlag(wrk&0xff);
			
			unsetFlag(FLAG_ADDSUB);
			
			if (((A&0xF)+(ac1.val&0xF))>0xF) setFlag(FLAG_HALFCARRY);
			else unsetFlag(FLAG_HALFCARRY);
			
			
			break;
		case ADDSPNN:
			// TODO: add flags.
			wrk = SP+convertSignedByte(ac1.val&0xff);
			
			//if ((this.register.sp^n^result)&0x100) this.setC(); else this.clearC();
			if ((( SP^convertSignedByte(ac1.val&0xff)^wrk)&0x100)>0) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			
			unsetFlag(FLAG_ZERO);
			unsetFlag(FLAG_ADDSUB);
			
			SP = wrk;
			
			break;
		case DAA:
			// TODO: !!
	        int correction = 0;
	        boolean flagN = testFlag(FLAG_ADDSUB);
	        boolean flagH = testFlag(FLAG_HALFCARRY);
	        boolean flagC = testFlag(FLAG_CARRY);
	        
	        if (flagH || (!flagN && (A & 0xF) > 9))
	            correction = 6;

	        if (flagC || (!flagN && A > 0x99)) {
	            correction |= 0x60;
	            setFlag(FLAG_CARRY);
	        }
	        
	        wrk = A;
	        wrk += flagN ? -correction : correction;
	        wrk &= 0xFF;
	        
	        unsetFlag(FLAG_HALFCARRY);

	        handleZeroFlag(wrk);
	        
			A=wrk;
			
			break;
		case ADDHL:
			wrk = getHL() + ac2.val;
			
			if (wrk>0xffff) setFlag(FLAG_CARRY); 
			else unsetFlag(FLAG_CARRY);
			
			unsetFlag(FLAG_ADDSUB);
			//if (((hl&0xFFF)+(value&0xFFF))&0x1000) this.setH(); else this.clearH();
			if ((( (getHL()&0xFFF)+(ac2.val&0xFFF) ) &0x1000)>0) setFlag(FLAG_HALFCARRY);
			else unsetFlag(FLAG_HALFCARRY);
			
			setHL(wrk&0xffff);
			
			break;
		case ADC:
			wrk = A + ac1.val + (testFlag(FLAG_CARRY)?1:0);

			handleZeroFlag(wrk&0xff);
			
			if (wrk>0xff) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			
			unsetFlag(FLAG_ADDSUB);
			
			if (((A^ac1.val^wrk)&0x10)>0) setFlag(FLAG_HALFCARRY);
			else unsetFlag(FLAG_HALFCARRY);
			
			A = wrk&0xff;
			
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
		case CCF:
			unsetFlag(FLAG_CARRY);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_HALFCARRY);
			break;
		case JRNZ:
			if (testFlag(FLAG_ZERO)==false) {
				jumpRelative(ac1.val);
			}
			break;
		case JRZ:
			if (testFlag(FLAG_ZERO)==true) {
				jumpRelative(ac1.val);
			}
			break;
		case JRNC:
			if (testFlag(FLAG_CARRY)==false) {
				jumpRelative(ac1.val);
			}
			break;
		case JRC:
			if (testFlag(FLAG_CARRY)==true) {
				jumpRelative(ac1.val);
			}
			break;
		case JR:
			jumpRelative(ac1.val);

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
		case JPC:
			if (testFlag(FLAG_CARRY)==true) {
				wrk = ac1.val;
				PC=wrk;
			}
			break;
		case LDZPGCA:
			mem.poke(0xff00+(C&0xff), A&0xff);
			break;
		case LDZPGNNA:
			mem.poke(0xff00+(ac1.val&0xff), A&0xff);
			break;
		case LDAZPGNN:
			A = mem.peek(0xff00+(ac1.val&0xff))&0xff;
			if (displayInstruction) System.out.println("zpg addr:"+Utils.toHex4(0xff00+ac1.val)+" val:"+A);
			break;
		case LDAZPGC:
			A = mem.peek(0xff00+(C&0xff))&0xff;
			//if (displayInstruction) System.out.println("zpg addr:"+Utils.toHex4(0xff00+ac1.val)+" val:"+A);
			break;
		case LDHLSPN:
			int signedByte = convertSignedByte(ac1.val&0xff);
			int ptr = SP+signedByte;
			
			wrk = combineBytes(mem.peek(ptr),mem.peek(ptr+1));
			
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_ZERO);
			
			 if (((SP^signedByte^wrk)&0x100)==0x100) setFlag(FLAG_CARRY);
			 else unsetFlag(FLAG_CARRY);
			 
		     if (((SP^signedByte^wrk)&0x10)==0x10) setFlag(FLAG_HALFCARRY);
		     unsetFlag(FLAG_HALFCARRY);
		        
			setHL(wrk);
			
			break;
		case RR:
			wrk = ac1.val;
			
			if ((wrk&1)>0) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_HALFCARRY);
			
			wrk = wrk>>1;
			handleZeroFlag(wrk&0xFF);
			
			mem.poke(ac1.addr, wrk&0xff);
			
			break;
		case RRCA:
			wrk = ac1.val;
			if (testFlag(FLAG_CARRY)) wrk+=0x100;

			if ((wrk&1)>0) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			
			unsetFlag(FLAG_ZERO);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_HALFCARRY);
			
			wrk = wrk>>1;
			mem.poke(ac1.addr, wrk&0xff);
			
			break;
		case RLA:
			A = A << 1;
			if (testFlag(FLAG_CARRY)) A|=1;
			if (A>0xff) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			A = A&0xff;
			
			unsetFlag(FLAG_ZERO);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_HALFCARRY);
			
			break;
		case RLCA:
			wrk = ac1.val;

			wrk = (wrk<<1)+(testFlag(FLAG_CARRY)?1:0);
			if (wrk>0xff) setFlag(FLAG_CARRY);
			else  unsetFlag(FLAG_CARRY);
			
			unsetFlag(FLAG_ZERO);
			unsetFlag(FLAG_ADDSUB);
			unsetFlag(FLAG_HALFCARRY);
			
			mem.poke(ac1.addr, wrk&0xff);
			break;
		case CALL:
			//pushW(entryPC+1);
			call(ac1.val);
			break;
		case CALLNZ:
			//pushW(entryPC+1);
			if (testFlag(FLAG_ZERO)==false) {
				call(ac1.val);
			}
			break;
		case CALLZ:
			if (testFlag(FLAG_ZERO)==true) {
				call(ac1.val);
			}
			break;
		case CALLC:
			if (testFlag(FLAG_CARRY)==true) {
				call(ac1.val);
			}
			break;
		case CALLNC:
			if (testFlag(FLAG_CARRY)!=true) {
				call(ac1.val);
			}
			break;
		case PUSHW:
			pushW(ac1.val);
			
			break;
		case POPW:
			wrk = popW();

			mem.poke(ac1.addr, wrk);
			break;

		case CP:
			wrk = A - ac1.val;
			
			handleZeroFlag(wrk&0xff);
			
			setFlag(FLAG_ADDSUB);
			if (A < ac1.val) setFlag(FLAG_CARRY);
			else unsetFlag(FLAG_CARRY);
			
			if ((A&0xF) < (ac1.val&0xF)) setFlag(FLAG_HALFCARRY);
			else unsetFlag(FLAG_HALFCARRY);
			
			/*
			 * const result = this.register.a - value;
	        if ((result&0xFF) === 0) this.setZ(); else this.clearZ();
	        this.setN();
	        if (this.register.a < value) this.setC(); else this.clearC();
	        if ((this.register.a&0xF) < (value&0xF)) this.setH(); else this.clearH();
			 */
			break;
		case CPL:
			// wrk = (~A)&0xff;
			//			// this.register.a ^= 0xFF;
			wrk = A^0xFF;
			
			setFlag(FLAG_ADDSUB);
			setFlag(FLAG_HALFCARRY);
			
			A = wrk;
			break;
			
		case RST_18H:
			jumpToInterrupt(0x0018);
			break;
		case RST_10H:
			jumpToInterrupt(0x0010);
			break;
		case RST_20H:
			jumpToInterrupt(0x0020);
			break;
		case RST_30H:
			jumpToInterrupt(0x0030);
			break;
		case RST_38H:
			jumpToInterrupt(0x0038);
			break;
		case RST_8H:
			jumpToInterrupt(0x0008);
			break;
		case RST_28H:
			jumpToInterrupt(0x0028);
			break;
		case RST_00H:
			jumpToInterrupt(0x0000);
			break;
			
		default:
			System.out.println("Unhandled instruction at "+Utils.toHex4(entryPC)+
					" : "+Utils.toHex2(currentInstruction)+ 
					" tick:"+tickCount);
			mem.poke(0xffff+10, 1);
			break;
		}

		// Handle interrupts before checking current instruction.
		//checkInterrupts();
		
	}
//int A,B,C,D,E,H,L;
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
	

	public void call(int addr) {
		pushW(PC);
		PC = addr;
	}
	
	// Set a flag on the interrupt register.
	public void requestInterrupt(int val) {
		this.mem.RAM[0xFF0F] |= val;
		//this.mmu.writeByte(0xFF0F, this.mmu.readByte(0xFF0F)|(1<<id));
	}

	public void checkInterrupts() {
		if (interruptEnabled==0) return;
		//interruptEnabled=0;
		
		boolean dbgMsgOn = false;
		
		int intEnabled = mem.RAM[0xFFFF];
		int intRequests = mem.RAM[0xFF0F];
		int masked = intEnabled&intRequests; 
		/*
              Bit 4: New Value on Selected Joypad Keyline(s) (rst 60)
              Bit 3: Serial I/O transfer end                 (rst 58)
              Bit 2: Timer Overflow                          (rst 50)
              Bit 1: LCD (see STAT)                          (rst 48)
              Bit 0: V-Blank                                 (rst 40)
		 */
		if ((masked&INT_VBLANK)>0) {
			halt=0;
			disableInterrupts();
			mem.RAM[0xFF0F] &= ~(INT_VBLANK); // Reset interrupt flag.
			if (dbgMsgOn) System.out.println("Detected interrupt VBLANK ########################################");
			jumpToInterrupt(0x0040); // VBLANK handler.
			return;
		}
	
		if ((masked&INT_LCDSTAT)>0) { // 1
			halt=0;
			disableInterrupts();
			mem.RAM[0xFF0F] &= ~(INT_LCDSTAT); // Reset interrupt flag.
			if (dbgMsgOn) System.out.println("Detected interrupt INT_LCDSTAT ########################################");
			jumpToInterrupt(0x0048);
			return;
		}
		if ((masked&INT_TIMER)>0) { // 2
			halt=0;
			disableInterrupts();
			mem.RAM[0xFF0F] &= ~(INT_TIMER); // Reset interrupt flag.
			if (dbgMsgOn) System.out.println("Detected interrupt INT_TIMER ########################################");
			jumpToInterrupt(0x0050);
			return;
		}
		if ((masked&INT_SERIAL)>0) { // 3
			halt=0;
			disableInterrupts();
			mem.RAM[0xFF0F] &= ~(INT_SERIAL); // Reset interrupt flag.
			if (dbgMsgOn) System.out.println("Detected interrupt INT_SERIAL ########################################");
			jumpToInterrupt(0x0058);
			return;
		}	
		if ((masked&INT_JOYPAD)>0) { // 4
			halt=0;
			disableInterrupts();
			mem.RAM[0xFF0F] &= ~(INT_JOYPAD); // Reset interrupt flag.
			if (dbgMsgOn) System.out.println("Detected interrupt VBLANK ########################################");
			jumpToInterrupt(0x0060);
			return;
		}
	}
	
	public void jumpToInterrupt(int addr) {
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
			//System.out.println("pHL="+Utils.toHex2(peeked));
			break;
		case __BC:
			peeked = mem.peek(getBC());
			ac.val = peeked;
			//ac.bytesRead += " " + Utils.toHex2(peeked)
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
		return combineBytes(A, FL&0xF0);
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
		this.FL = getLowByte(val)&0xF0;
	}
	
	public void enableInterrupts() {
		pendingEnableInterrupt = 2;
		//interruptEnabled=1; 
	}
	
	public void disableInterrupts() {
		pendingDisableInterrupt = 2;
		//interruptEnabled=0;
	}
	
	public void handleZeroFlag(int val) {
		if ((val&0xff)==0) setFlag(FLAG_ZERO);
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
		//mem.poke(SP--, getHighByte(val));
		//mem.poke(SP--, getLowByte(val));
		mem.RAM[SP--] = getHighByte(val);
		mem.RAM[SP--] = getLowByte(val);
	}

	public int popW() {
		// int lb = mem.peek(++SP);
		// int hb = mem.peek(++SP);
		int lb = mem.RAM[++SP];
		int hb = mem.RAM[++SP];

		
		return combineBytes(hb, lb);
	}
}
