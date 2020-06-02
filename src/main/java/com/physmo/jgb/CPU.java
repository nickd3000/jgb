package com.physmo.jgb;

public class CPU {

    // CPU Flags
    public static final int FLAG_ZERO = 0b1000_0000;
    public static final int FLAG_ADDSUB = 0b0100_0000;
    public static final int FLAG_HALFCARRY = 0b0010_0000;
    public static final int FLAG_CARRY = 0b0001_0000;

    // Interrupts
    public static int INT_VBLANK = 0b0000_0001; // Vblank off Vblank on
    public static int INT_LCDSTAT = 0b0000_0010; // LCD stat off LCD stat on
    public static int INT_TIMER = 0b0000_0100; // Timer off Timer on
    public static int INT_SERIAL = 0b0000_1000; // Serial off Serial on
    public static int INT_JOYPAD =  0b0001_0000; // Joypad off

    public static boolean displayInstruction = false;
    public HARDWARE_TYPE hardwareType = HARDWARE_TYPE.CGB;
    public INPUT input = null;
    public boolean speedMode = false;
    MEM mem = null;
    GPU gpu = null;

    // Registers.
    int A, B, C, D, E, H, L;
    int PC; // Program counter
    int SP = 0xFFFE; // Stack pointer
    int FL;
    int cycles = 0;
    int tickCount = 0;
    int interruptEnabled = 0;
    int halt = 0;
    int pendingEnableInterrupt = 0;
    int pendingDisableInterrupt = 0;
    int fakeVerticalBlank = 0;
    int topOfSTack = 0xFFFE; // used for debugging.

    AddressContainer ac1 = new AddressContainer();
    AddressContainer ac2 = new AddressContainer();
    int serialBit = 0;

    public void attachHardware(MEM mem, INPUT input, GPU gpu) {
        this.mem = mem;
        this.input = input;
        this.gpu = gpu;
    }

    public MEM getMem() {
        return mem;
    }

    public void tick() {
        // mem.poke(0xC000,0x69);

        tickCount++;
        // if (tickCount>30000000) displayInstruction=true;
        //if (tickCount>6020435 - 10000) displayInstruction=true;
        // if (tickCount>2855623-1000) displayInstruction=true;
        // if (PC==0x001D) displayInstruction=true;
        // if (PC>0x00FF) displayInstruction=true;
        //displayInstruction=true;
        // if (PC==0x0100) displayInstruction=true;
        // if (PC==0x02A0) displayInstruction=true;

//		if (PC == 0xCBB0) {
//			System.out.println("elephants "+tickCount);
//		}


        int serialBitOld = serialBit;
        if ((mem.peek(0xFF02) & 0x10) > 0)
            serialBit = 1;
        else
            serialBit = 0;

        if (serialBitOld != serialBit)
            System.out.println("" + (char) mem.peek(MEM.ADDR_FF01_SERIAL_DATA));


        if (mem.peek(0xdef6) > 0xff) {
            System.out.println("overflow detected at tick pc:" + Utils.toHex4(PC) + "  tick:" + tickCount);
        }

        if ((SP >= 0xA000) && (SP <= 0xBFFF)) {
            //System.out.println("Stack pointing to paged memory??");
        }

        Debug.checkRegisters(this);

        // Handle interrupts before checking current instruction.
        checkInterrupts();

        if (halt == 1)
            return;

        int entryPC = PC;
        int currentInstruction = mem.peek(PC++);

        if (mem.peek(0xf6de) > 0xff) {
            System.out.println("0xf6de is overflow!!! val:" + Utils.toHex4(mem.peek(0xf6de)));
        }

        // Get this instructions definition.
        InstructionDefinition def = InstructionDefinition.getEnumFromId(currentInstruction & 0xff);

        if (def == null) {
            System.out.println("No InstructionDefinition at " + Utils.toHex4(entryPC) + " : "
                    + Utils.toHex2(currentInstruction) + "   " + "tick:" + tickCount);
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
        int carryOut = 0;
        int carryIn = 0;

        // Handle buffered interrupt enable/disable;
        if (pendingDisableInterrupt > 0) {
            pendingDisableInterrupt--;
            if (pendingDisableInterrupt == 0) {
                interruptEnabled = 0;
            }
        }
        if (pendingEnableInterrupt > 0) {
            pendingEnableInterrupt--;
            if (pendingEnableInterrupt == 0) {
                interruptEnabled = 1;
            }
        }

        switch (command) {
            case NOP:
                break;
            case STOP:
                // Used by GBC to change speed.
                wrk = mem.peek(0xFF4d);

                if ((wrk & 1) > 0) {
                    speedMode = !speedMode;

                    if (speedMode) wrk = 0b10000000;
                    else wrk = 0b00000000;
                }

                mem.poke(0xff4d, wrk);

                break;
            case HALT:
                if (interruptEnabled == 1)
                    halt = 1;
                else {
                    PC++;
                }
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
                if (wrk > 0xff)
                    wrk = 0;

                handleZeroFlag(wrk & 0xff);
                unsetFlag(FLAG_ADDSUB);

                if ((ac1.val & 0xF) + 1 > 0xF)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                mem.poke(ac1.addr, wrk & 0xff);

                break;
            case INCW:
                wrk = ac1.val + 1;
                if (wrk > 0xffff)
                    wrk = 0;
                mem.poke(ac1.addr, wrk & 0xffff);
                break;
            case DEC:
                wrk = ac1.val - 1;
                if (wrk < 0)
                    wrk = 0xff;

                handleZeroFlag(wrk);
                setFlag(FLAG_ADDSUB);
                if ((ac1.val & 0xF) - 1 < 0)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                mem.poke(ac1.addr, wrk & 0xff);

                break;
            case DECW:
                wrk = ac1.val - 1;
                if (wrk < 0)
                    wrk = 0xffff;
                mem.poke(ac1.addr, wrk);
                break;
            case LDSPHL:
                // TODO: this is new
                SP = getHL();
                // SP = ((H&0xff)<<8)|(L&0xff);
                break;
            case LD:
                wrk = ac2.val;
                if (displayInstruction)
                    System.out.println("addr2:" + Utils.toHex4(ac2.addr) + " val2:" + ac2.val);
                mem.poke(ac1.addr, ac2.val);
                break;
            case LDW: // LD Word
                wrk = ac2.val;
                if (ac1.mode == ADDRMODE.__nnnn) {
                    mem.poke(ac1.addr, getLowByte(wrk));
                    mem.poke(ac1.addr + 1, getHighByte(wrk));
                }
                break;
            case LDD: // Load and decrement?
                wrk = ac2.val;
                mem.poke(ac1.addr, ac2.val);

                if (def.getAddressMode1() == ADDRMODE.__HL) {
                    setHL(getHL() - 1);
                } else if (def.getAddressMode2() == ADDRMODE.__HL) {
                    setHL(getHL() - 1);
                } else {
                    System.out.println("ERROR LDD");
                }
                if (getHL() < 0)
                    setHL(0xffff);

                break;
            case LDI: // Load and decrement?
                wrk = ac2.val;
                mem.poke(ac1.addr, ac2.val);

                if (def.getAddressMode1() == ADDRMODE.__HL) {
                    setHL(getHL() + 1);
                } else if (def.getAddressMode2() == ADDRMODE.__HL) {
                    setHL(getHL() + 1);
                } else {
                    System.out.println("ERROR LDI");
                }

                if (getHL() > 0xffff)
                    setHL(0x0);

                break;
            case XOR:
                wrk = A ^ ac1.val;
                handleZeroFlag(wrk & 0xff);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_CARRY);
                unsetFlag(FLAG_HALFCARRY);
                A = wrk & 0xff;
                break;
            case OR:
                wrk = A | ac1.val;
                handleZeroFlag(wrk & 0xff);
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
                if (displayInstruction)
                    System.out.println("and val:" + Utils.toHex2(ac1.val));
                break;
            case SUB:
                wrk = A - ac1.val;

                if (ac1.val > A)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                handleZeroFlag(wrk & 0xff);
                setFlag(FLAG_ADDSUB);

                if (((A ^ ac1.val ^ wrk) & 0x10) > 0)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                A = wrk & 0xff;

                break;
            case ADD:
                wrk = A + ac1.val;

                if (wrk > 0xff)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);
                handleZeroFlag(wrk & 0xff);

                unsetFlag(FLAG_ADDSUB);

                if (((A & 0xF) + (ac1.val & 0xF)) > 0xF)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                A = wrk & 0xff;

                break;
            case ADDSPNN:

                wrk = SP + convertSignedByte(ac1.val & 0xff);

                if (((SP ^ convertSignedByte(ac1.val & 0xff) ^ wrk) & 0x10) > 0)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                if (((SP ^ convertSignedByte(ac1.val & 0xff) ^ wrk) & 0x100) > 0)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                unsetFlag(FLAG_ZERO);
                unsetFlag(FLAG_ADDSUB);

                SP = wrk;

                break;
            case DAA:

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

                A = wrk;

                break;
            case ADDHL:
                wrk = getHL() + ac2.val;

                if (wrk > 0xffff)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                unsetFlag(FLAG_ADDSUB);

                if ((((getHL() & 0xFFF) + (ac2.val & 0xFFF)) & 0x1000) > 0)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                setHL(wrk & 0xffff);

                break;
            case ADC:
                wrk = A + ac1.val + (testFlag(FLAG_CARRY) ? 1 : 0);

                handleZeroFlag(wrk & 0xff);

                if (wrk > 0xff)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                unsetFlag(FLAG_ADDSUB);

                if (((A ^ ac1.val ^ wrk) & 0x10) > 0)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                A = wrk & 0xff;

                break;
            case SBC:

                wrk = A - ((ac1.val & 0xff) + (testFlag(FLAG_CARRY) ? 1 : 0));

                if ((wrk & 0xFF) > 0) unsetFlag(FLAG_ZERO);
                else setFlag(FLAG_ZERO);
                if ((wrk & 0x100) > 0) setFlag(FLAG_CARRY);
                else unsetFlag(FLAG_CARRY);
                if (((A ^ ac1.val ^ wrk) & 0x10) != 0) setFlag(FLAG_HALFCARRY);
                else unsetFlag(FLAG_HALFCARRY);

                setFlag(FLAG_ADDSUB);

                A = wrk & 0xff;
                break;
            case PREFIX:
                CPUPrefixInstructions.processPrefixCommand(this, ac1.val);
                break;
            case SCF:
                setFlag(FLAG_CARRY);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);
                break;
            case CCF:
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);

                if ((FL & 0x10) > 0)
                    unsetFlag(FLAG_CARRY);
                else
                    setFlag(FLAG_CARRY);

                break;
            case JRNZ:
                if (!testFlag(FLAG_ZERO)) {
                    jumpRelative(ac1.val);
                }
                break;
            case JRZ:
                if (testFlag(FLAG_ZERO)) {
                    jumpRelative(ac1.val);
                }
                break;
            case JRNC:
                if (!testFlag(FLAG_CARRY)) {
                    jumpRelative(ac1.val);
                }
                break;
            case JRC:
                if (testFlag(FLAG_CARRY)) {
                    jumpRelative(ac1.val);
                }
                break;
            case JR:
                jumpRelative(ac1.val);

                break;
            case JPZ:
                if (testFlag(FLAG_ZERO)) {
                    wrk = ac1.val;
                    PC = wrk;
                }
                break;
            case JPNZ:
                if (!testFlag(FLAG_ZERO)) {
                    wrk = ac1.val;
                    PC = wrk;
                }
                break;
            case JPNC:
                if (!testFlag(FLAG_CARRY)) {
                    wrk = ac1.val;
                    PC = wrk;
                }
                break;
            case JPC:
                if (testFlag(FLAG_CARRY)) {
                    wrk = ac1.val;
                    PC = wrk;
                }
                break;

            case LDZPGCA:
                mem.poke(0xff00 + (C & 0xff), A & 0xff);
                // mem.RAM[0xff00+(C&0xff)] = A&0xff;
                break;
            case LDZPGNNA:
                mem.poke(0xff00 + (ac1.val & 0xff), A & 0xff);
                // mem.RAM[0xff00+(ac1.val&0xff)] = A&0xff;
                break;
            case LDAZPGNN:
                A = mem.peek(0xff00 + (ac1.val & 0xff)) & 0xff;
                // A = mem.RAM[(0xff00+(ac1.val&0xff))]&0xff;
                if (displayInstruction)
                    System.out.println("zpg addr:" + Utils.toHex4(0xff00 + ac1.val) + " val:" + A);
                break;
            case LDAZPGC:
                A = mem.peek(0xff00 + (C & 0xff)) & 0xff;
                break;

            case LDHLSPN:
                int signedByte = convertSignedByte(ac2.val & 0xff);
                int ptr = SP + signedByte;

                wrk = ptr & 0xffff;

                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_ZERO);

                if (((SP ^ signedByte ^ wrk) & 0x100) == 0x100)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                if (((SP ^ signedByte ^ wrk) & 0x10) == 0x10)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                setHL(wrk);

                break;
            case RR:

                carryIn = testFlag(FLAG_CARRY) ? 1 : 0;
                carryOut = ((ac1.val & 0x01) > 0) ? 1 : 0;

                wrk = (ac1.val >> 1) + (carryIn << 7);

                if (carryOut == 1)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);
                unsetFlag(FLAG_ZERO);

                mem.poke(ac1.addr, wrk & 0xff);

                break;
            case RRCA:

                carryOut = ((A & 0x01) > 0) ? 1 : 0;

                if (carryOut == 1) setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                wrk = (A >> 1) + (carryOut << 7);

                unsetFlag(FLAG_ZERO);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);

                mem.poke(ac1.addr, wrk & 0xff);
                break;
            case RLA:
                A = A << 1;
                if (testFlag(FLAG_CARRY))
                    A |= 1;
                if (A > 0xff)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);
                A = A & 0xff;

                unsetFlag(FLAG_ZERO);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);

                break;
            case RLCA:

                carryOut = ((A & 0x80) > 0) ? 1 : 0;

                if (carryOut == 1) setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                wrk = (A << 1) + carryOut;

                unsetFlag(FLAG_ZERO);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);

                mem.poke(ac1.addr, wrk & 0xff);
                break;
            case CALL:
                call(ac1.val);
                break;
            case CALLNZ:
                if (!testFlag(FLAG_ZERO)) {
                    call(ac1.val);
                }
                break;
            case CALLZ:
                if (testFlag(FLAG_ZERO)) {
                    call(ac1.val);
                }
                break;
            case CALLC:
                if (testFlag(FLAG_CARRY)) {
                    call(ac1.val);
                }
                break;
            case CALLNC:
                if (!testFlag(FLAG_CARRY)) {
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

                handleZeroFlag(wrk & 0xff);

                setFlag(FLAG_ADDSUB);
                if (A < ac1.val)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                if ((A & 0xF) < (ac1.val & 0xF))
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                break;
            case CPL:
                wrk = A ^ 0xFF;
                setFlag(FLAG_ADDSUB);
                setFlag(FLAG_HALFCARRY);
                A = wrk & 0xff;
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
                System.out.println("Unhandled instruction at " + Utils.toHex4(entryPC) + " : "
                        + Utils.toHex2(currentInstruction) + " tick:" + tickCount);
                mem.poke(0xffff + 10, 1);
                break;
        }

    }


    public void call(int addr) {
        pushW(PC);
        PC = addr;
    }

    public void ret() {
        PC = popW();
    }

    // Set a flag on the interrupt register.
    public void requestInterrupt(int val) {
        this.mem.RAM[0xFF0F] |= val;
    }

    public void checkInterrupts() {
        if (interruptEnabled == 0)
            return;

        boolean dbgMsgOn = false;

        int intEnabled = mem.RAM[0xFFFF];
        int intRequests = mem.RAM[0xFF0F];
        int masked = intEnabled & intRequests;

        if ((masked & INT_VBLANK) > 0) {
            halt = 0;
            disableInterrupts();
            mem.RAM[0xFF0F] &= ~(INT_VBLANK); // Reset interrupt flag.
            if (dbgMsgOn)
                System.out.println("Detected interrupt VBLANK ########################################");
            jumpToInterrupt(0x0040); // VBLANK handler.
            return;
        }

        if ((masked & INT_LCDSTAT) > 0) { // 1
            halt = 0;
            disableInterrupts();
            mem.RAM[0xFF0F] &= ~(INT_LCDSTAT); // Reset interrupt flag.
            if (dbgMsgOn)
                System.out.println("Detected interrupt INT_LCDSTAT ########################################");
            jumpToInterrupt(0x0048);
            return;
        }
        if ((masked & INT_TIMER) > 0) { // 2
            halt = 0;
            disableInterrupts();
            mem.RAM[0xFF0F] &= ~(INT_TIMER); // Reset interrupt flag.
            if (dbgMsgOn)
                System.out.println("Detected interrupt INT_TIMER ########################################");
            jumpToInterrupt(0x0050);
            return;
        }
        if ((masked & INT_SERIAL) > 0) { // 3
            halt = 0;
            disableInterrupts();
            mem.RAM[0xFF0F] &= ~(INT_SERIAL); // Reset interrupt flag.
            if (dbgMsgOn)
                System.out.println("Detected interrupt INT_SERIAL ########################################");
            jumpToInterrupt(0x0058);
            return;
        }
        if ((masked & INT_JOYPAD) > 0) { // 4
            halt = 0;
            disableInterrupts();
            mem.RAM[0xFF0F] &= ~(INT_JOYPAD); // Reset interrupt flag.
            if (dbgMsgOn)
                System.out.println("Detected interrupt VBLANK ########################################");
            jumpToInterrupt(0x0060);
            return;
        }
    }

    public void jumpToInterrupt(int addr) {
        pushW(PC);
        PC = addr;
    }

    // Based on the address mode, set up the address container
    // with data and addresses to be used by the current operation.
    public void processAddressMode(AddressContainer ac, ADDRMODE mode) {
        int peeked = 0;

        ac.mode = mode;
        ac.addr = AddrMap.ADDR_INVALID;

        switch (mode) {
            case NONE:
                break;
            case nn:
                ac.val = getNextByte();
                break;
            case nnnn:
                ac.val = getNextWord();
                break;
            case A:
                ac.val = A;
                ac.addr = AddrMap.ADDR_A;
                break;
            case B:
                ac.val = B;
                ac.addr = AddrMap.ADDR_B;
                break;
            case C:
                ac.val = C;
                ac.addr = AddrMap.ADDR_C;
                break;
            case D:
                ac.val = D;
                ac.addr = AddrMap.ADDR_D;
                break;
            case E:
                ac.val = E;
                ac.addr = AddrMap.ADDR_E;
                break;
            case H:
                ac.val = H;
                ac.addr = AddrMap.ADDR_H;
                break;
            case L:
                ac.val = L;
                ac.addr = AddrMap.ADDR_L;
                break;

            case SP:
                ac.val = SP;
                ac.addr = AddrMap.ADDR_SP;
                break;
            case HL:
                ac.val = getHL();
                ac.addr = AddrMap.ADDR_HL;
                break;
            case BC:
                ac.val = getBC();
                ac.addr = AddrMap.ADDR_BC;
                break;
            case DE:
                ac.val = getDE();
                ac.addr = AddrMap.ADDR_DE;
                break;
            case AF:
                ac.val = getAF();
                ac.addr = AddrMap.ADDR_AF;
                break;
            case __HL:
                ac.val = mem.peek(getHL());
                ac.addr = getHL();
                break;
            case __BC:
                ac.val = mem.peek(getBC());
                ac.addr = getBC();
                break;
            case __DE:
                ac.val = mem.peek(getDE());
                ac.addr = getDE();
                break;
            case __nnnn:
                peeked = getNextWord();
                ac.val = mem.peek(peeked);
                ac.addr = peeked;
                break;

            default:
                System.out.println("Address mode not recognised in processAddressMode : " + mode.toString());
                break;
        }

    }

    public void jumpRelative(int val) {

        int tc = convertSignedByte(val); // & 0xff);

        int move = val;
        if (move > 127) move = -((~move + 1) & 0xFF);

        if (tc != move) {
            for (int i = 0; i < 1000; i++) {
                System.out.println("FUCK");
            }
        }

        if (displayInstruction)
            System.out.println("Jump relative by " + tc + " to " + Utils.toHex4(PC + tc));

//		System.out.println("Jump relative "+
//				"  val:" + val +
//				"  signed:" + tc +
//				"  move:" + move +
//				"  PC " + Utils.toHex4(PC) +
//				"  PC+tc " + Utils.toHex4((PC + tc)) +
//				"  "+Debug.getRegisters(this));

        PC = (PC + move) & 0xffff;

    }


    // Combine two bytes into one 16 bit value.
    public int combineBytes(int h, int l) {
        return ((h & 0xff) << 8) | (l & 0xff);
    }

    public int getHighByte(int val) {
        return (val >> 8) & 0xff;
    }

    public int getLowByte(int val) {
        return val & 0xff;
    }

    public int convertSignedByte(int val) {
        if ((val & 0b1000_0000) > 0) {
            return -1 - ((~val) & 0xff);
        }
        return val;
    }

    public int getAF() {
        return combineBytes(A, FL & 0xF0);
    }

    public void setAF(int val) {
        this.A = getHighByte(val);
        this.FL = getLowByte(val) & 0xF0;
    }

    public int getBC() {
        return combineBytes(B, C);
    }

    public void setBC(int val) {
        this.B = getHighByte(val);
        this.C = getLowByte(val);
    }

    public int getDE() {
        return combineBytes(D, E);
    }

    public void setDE(int val) {
        this.D = getHighByte(val);
        this.E = getLowByte(val);
    }

    public int getHL() {
        return combineBytes(H, L);
    }

    public void setHL(int val) {
        this.H = getHighByte(val);
        this.L = getLowByte(val);
    }

    public void setHL(int _h, int _l) {
        this.H = _h & 0xff;
        this.L = _l & 0xff;
    }

    public void enableInterrupts() {
        pendingEnableInterrupt = 1;
        // interruptEnabled=1;
    }

    public void disableInterrupts() {
        pendingDisableInterrupt = 1;
    }

    public void handleZeroFlag(int val) {
        if ((val & 0xffff) == 0)
            setFlag(FLAG_ZERO);
        else
            unsetFlag(FLAG_ZERO);
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


    public void setFlag(int flag) {
        FL |= flag;
    }

    public void unsetFlag(int flag) {
        FL &= ~(flag);
    }

    public boolean testFlag(int flag) {
        return (FL & flag) > 0;
    }

    // STACK
    public void pushW(int val) {
        mem.poke((--SP) & 0xffff, getHighByte(val));
        mem.poke((--SP) & 0xffff, getLowByte(val));
    }

    public int popW() {
        int lb = mem.peek((SP++) & 0xffff);
        int hb = mem.peek((SP++) & 0xffff);
        return combineBytes(hb, lb);
    }
}
