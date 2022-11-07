package com.physmo.jgb;

import com.physmo.jgb.microcode.MicroOp;
import com.physmo.jgb.microcode.Microcode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static int INT_JOYPAD = 0b0001_0000; // Joypad off

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


    int serialBit = 0;

    Microcode microcode = new Microcode();
    int tempRegister = 0;
    int addressBuffer = 0;

    Map<Integer, Integer> instructionCounts = new HashMap<>();

    int printNewOpUseCount = 0;
    int lastInstruction = 0;

    public void attachHardware(MEM mem, INPUT input, GPU gpu) {
        this.mem = mem;
        this.input = input;
        this.gpu = gpu;
    }

    public MEM getMem() {
        return mem;
    }

    public void tick() {

        tickCount++;

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

        ////////////////////////////////////////////////////
        // Inject new microcode stuff
        MicroOp[] ops = microcode.getInstructionCode(currentInstruction);
        if (ops.length > 0 && ops[0] != MicroOp.TODO) {
            //printNewOpUse(currentInstruction);
            for (MicroOp op : ops) {
                doMicroOp(op);
            }
        }
        lastInstruction = currentInstruction;

    }

    public int getLastInstruction() {
        return lastInstruction;
    }

    private void printNewOpUse(int currentInstruction) {

        if (!instructionCounts.containsKey(currentInstruction)) {
            instructionCounts.put(currentInstruction, 1);
            String instructionName = microcode.getInstructionName(currentInstruction);
            System.out.println("Microcode: " + instructionName);
        } else {
            int count = instructionCounts.get(currentInstruction);
            instructionCounts.put(currentInstruction, count + 1);
        }

        printNewOpUseCount++;
        if (printNewOpUseCount % 50000 == 0) {
            printSortedInstructionCounts();
        }
    }

    public void printSortedInstructionCounts() {
        class Pair {
            public int a;
            public int b;
            public Pair(int a, int b) {
                this.a = a;
                this.b = b;
            }
        }

        List<Pair> sortedList = new ArrayList<>();
        for (Integer key : instructionCounts.keySet()) {
            sortedList.add(new Pair(key, instructionCounts.get(key)));
        }

        sortedList.sort(Comparator.comparingInt(o -> o.b));
        System.out.println("------------------------------------------------");
        for (Pair pair : sortedList) {
            System.out.println(">> " + microcode.getInstructionName(pair.a) + "  " + pair.b);
        }

    }


    private void doMicroOp(MicroOp op) {
        switch (op) {
            case HALT:
                if (interruptEnabled == 1)
                    halt = 1;
                else {
                    PC++;
                }
                break;
            case NOP:
                break;
            case FETCH_A:
                tempRegister = A;
                break;
            case FETCH_B:
                tempRegister = B;
                break;
            case FETCH_C:
                tempRegister = C;
                break;
            case FETCH_D:
                tempRegister = D;
                break;
            case FETCH_E:
                tempRegister = E;
                break;
            case FETCH_H:
                tempRegister = H;
                break;
            case FETCH_L:
                tempRegister = L;
                break;
            case FETCH_AF:
                tempRegister = combineBytes(A, FL);
                break;
            case FETCH_BC:
                tempRegister = combineBytes(B, C);
                break;
            case FETCH_DE:
                tempRegister = combineBytes(D, E);
                break;
            case FETCH_HL:
                tempRegister = combineBytes(H, L);
                break;
            case FETCH_SP:
                tempRegister = SP;
                break;
            case FETCH_8:
                tempRegister = getNextByte();
                break;
            case FETCH_16:
                tempRegister = getNextWord();
                break;
            case FETCH_16_ADDRESS:
                addressBuffer = getNextWord();
                break;
            case SET_ADDR_FROM_HL:
                addressBuffer = getHL();
                break;
            case SET_ADDR_FROM_HL_INC:
                addressBuffer = getHL();
                setHL(getHL() + 1);
                break;
            case SET_ADDR_FROM_HL_DEC:
                addressBuffer = getHL();
                setHL(getHL() - 1);
                break;
            case SET_ADDR_FROM_BC:
                addressBuffer = getBC();
                break;
            case SET_ADDR_FROM_DE:
                addressBuffer = getDE();
                break;
            case FETCH_BYTE_FROM_ADDR:
                tempRegister = mem.peek(addressBuffer);
                break;
            case STORE_BYTE_AT_ADDRESS:
                mem.poke(addressBuffer, tempRegister);
                break;
            case STORE_A:
                A = tempRegister;
                break;
            case STORE_B:
                B = tempRegister;
                break;
            case STORE_C:
                C = tempRegister;
                break;
            case STORE_D:
                D = tempRegister;
                break;
            case STORE_E:
                E = tempRegister;
                break;
            case STORE_H:
                H = tempRegister;
                break;
            case STORE_L:
                L = tempRegister;
                break;
            case STORE_BC:
                setBC(tempRegister);
                break;
            case STORE_DE:
                setDE(tempRegister);
                break;
            case STORE_HL:
                setHL(tempRegister);
                break;
            case STORE_AF:
                setAF(tempRegister);
                break;
            case STORE_SP:
                SP = tempRegister;
                break;
            case STORE_p16WORD:
                mem.poke(addressBuffer, getLowByte(tempRegister));
                mem.poke(addressBuffer + 1, getHighByte(tempRegister));
                break;
            case INC_8:
                tempRegister = tempRegister + 1;
                if (tempRegister > 0xff)
                    tempRegister = 0;

                handleZeroFlag(tempRegister & 0xff);
                unsetFlag(FLAG_ADDSUB);

                if ((tempRegister & 0xF) + 1 > 0xF)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                break;
            case INC_16:
                tempRegister = tempRegister + 1;
                if (tempRegister > 0xffff)
                    tempRegister = 0;
                break;
            case DEC_8:
                tempRegister = tempRegister - 1;
                if (tempRegister < 0)
                    tempRegister = 0xff;

                handleZeroFlag(tempRegister);
                setFlag(FLAG_ADDSUB);
                if ((tempRegister & 0xF) - 1 < 0)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                break;
            case DEC_16:
                tempRegister = tempRegister - 1;
                if (tempRegister < 0)
                    tempRegister = 0xffff;
                break;
            case ADD_HL:
                tempRegister = getHL() + tempRegister;

                if (tempRegister > 0xffff)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                unsetFlag(FLAG_ADDSUB);

                //if ((((getHL() & 0xFFF) + (ac2.val & 0xFFF)) & 0x1000) > 0)
                if ((((getHL() & 0xFFF) + (tempRegister & 0xFFF)) & 0x1000) > 0)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                break;
            case ADD:
                int wrk = A + tempRegister;

                if (wrk > 0xff)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);
                handleZeroFlag(wrk & 0xff);

                unsetFlag(FLAG_ADDSUB);

                if (((A & 0xF) + (tempRegister & 0xF)) > 0xF)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                A = wrk & 0xff;

                break;
            case ADC:
                wrk = A + tempRegister + (testFlag(FLAG_CARRY) ? 1 : 0);

                handleZeroFlag(wrk & 0xff);

                if (wrk > 0xff)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                unsetFlag(FLAG_ADDSUB);

                if (((A ^ tempRegister ^ wrk) & 0x10) > 0)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                A = wrk & 0xff;

                break;
            case SUB:
                wrk = A - tempRegister;

                if (tempRegister > A)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                handleZeroFlag(wrk & 0xff);
                setFlag(FLAG_ADDSUB);

                if (((A ^ tempRegister ^ wrk) & 0x10) > 0)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                A = wrk & 0xff;

                break;
            case SBC:
                wrk = A - ((tempRegister & 0xff) + (testFlag(FLAG_CARRY) ? 1 : 0));

                if ((wrk & 0xFF) > 0) unsetFlag(FLAG_ZERO);
                else setFlag(FLAG_ZERO);
                if ((wrk & 0x100) > 0) setFlag(FLAG_CARRY);
                else unsetFlag(FLAG_CARRY);
                if (((A ^ tempRegister ^ wrk) & 0x10) != 0) setFlag(FLAG_HALFCARRY);
                else unsetFlag(FLAG_HALFCARRY);

                setFlag(FLAG_ADDSUB);

                A = wrk & 0xff;
                break;
            case AND:
                wrk = A & tempRegister;
                A = wrk;

                handleZeroFlag(wrk);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_CARRY);
                setFlag(FLAG_HALFCARRY);
                if (displayInstruction)
                    System.out.println("and val:" + Utils.toHex2(tempRegister));
                break;
            case XOR:
                wrk = A ^ tempRegister;
                handleZeroFlag(wrk & 0xff);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_CARRY);
                unsetFlag(FLAG_HALFCARRY);
                A = wrk & 0xff;
                break;
            case OR:
                wrk = A | tempRegister;
                handleZeroFlag(wrk & 0xff);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_CARRY);
                unsetFlag(FLAG_HALFCARRY);
                A = wrk & 0xff;
                break;
            case CP:
                wrk = A - tempRegister;

                handleZeroFlag(wrk & 0xff);

                setFlag(FLAG_ADDSUB);
                if (A < tempRegister)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                if ((A & 0xF) < (tempRegister & 0xF))
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                break;
//            case CPL:
//                wrk = A ^ 0xFF;
//                setFlag(FLAG_ADDSUB);
//                setFlag(FLAG_HALFCARRY);
//                A = wrk & 0xff;
//                break;
            case RLCA: // Rotate left with carry for A
                int carryOut = ((A & 0x80) > 0) ? 1 : 0;

                if (carryOut == 1) setFlag(FLAG_CARRY);
                else unsetFlag(FLAG_CARRY);

                tempRegister = (A << 1) + carryOut;
                unsetFlag(FLAG_ZERO);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);
                A = tempRegister & 0xff;

                break;
            case RRCA:
                carryOut = ((A & 0x01) > 0) ? 1 : 0;
                if (carryOut == 1) setFlag(FLAG_CARRY);
                else unsetFlag(FLAG_CARRY);
                tempRegister = (A >> 1) + (carryOut << 7);
                unsetFlag(FLAG_ZERO);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);
                A = tempRegister;
                break;
            case RL:
                tempRegister = tempRegister << 1;
                if (testFlag(FLAG_CARRY))
                    tempRegister |= 1;
                if (tempRegister > 0xff)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);
                tempRegister = tempRegister & 0xff;

                unsetFlag(FLAG_ZERO);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);

                break;
            case RRA:

                int carryIn = testFlag(FLAG_CARRY) ? 1 : 0;
                carryOut = ((A & 0x01) > 0) ? 1 : 0;

                A = (A >> 1) + (carryIn << 7);

                if (carryOut == 1)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);
                unsetFlag(FLAG_ZERO);

                break;
            case JRNZ:
                if (!testFlag(FLAG_ZERO)) {
                    jumpRelative(tempRegister);
                }
                break;
            case JRZ:
                if (testFlag(FLAG_ZERO)) {
                    jumpRelative(tempRegister);
                }
                break;
            case JRNC:
                if (!testFlag(FLAG_CARRY)) {
                    jumpRelative(tempRegister);
                }
                break;
            case JRC:
                if (testFlag(FLAG_CARRY)) {
                    jumpRelative(tempRegister);
                }
                break;
            case JR:
                jumpRelative(tempRegister);

                break;
            case JPZ:
                if (testFlag(FLAG_ZERO)) {
                    PC = addressBuffer;
                }
                break;
            case JPNZ:
                if (!testFlag(FLAG_ZERO)) {
                    PC = addressBuffer;
                }
                break;
            case JPNC:
                if (!testFlag(FLAG_CARRY)) {
                    PC = addressBuffer;
                }
                break;
            case JPC:
                if (testFlag(FLAG_CARRY)) {
                    PC = addressBuffer;
                }
                break;
            case JP:
                PC = addressBuffer;
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
                    PC = popW();
                }
                break;
            case RETC:
                if (testFlag(FLAG_CARRY)) {
                    PC = popW();
                }
                break;
            case DAA: // Decimal Adjust Accumulator to get a correct BCD representation after an arithmetic instruction

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

                tempRegister = A;
                tempRegister += flagN ? -correction : correction;
                tempRegister &= 0xFF;

                unsetFlag(FLAG_HALFCARRY);

                handleZeroFlag(tempRegister);

                A = tempRegister;

                break;
            case CPL: // ComPLement accumulator (A = ~A).
                tempRegister = A ^ 0xFF;
                setFlag(FLAG_ADDSUB);
                setFlag(FLAG_HALFCARRY);
                A = tempRegister & 0xff;
                break;
            case SCF: // Set carry flag
                setFlag(FLAG_CARRY);
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);
                break;
            case CCF: // Clear carry flag
                unsetFlag(FLAG_ADDSUB);
                unsetFlag(FLAG_HALFCARRY);
                if ((FL & 0x10) > 0)
                    unsetFlag(FLAG_CARRY);
                else
                    setFlag(FLAG_CARRY);
                break;
            case PUSHW:
                pushW(tempRegister);
                break;
            case POPW:
                tempRegister = popW();
                break;
            case CALL:
                call(addressBuffer);
                break;
            case CALLNZ:
                if (!testFlag(FLAG_ZERO)) {
                    call(addressBuffer);
                }
                break;
            case CALLZ:
                if (testFlag(FLAG_ZERO)) {
                    call(addressBuffer);
                }
                break;
            case CALLC:
                if (testFlag(FLAG_CARRY)) {
                    call(addressBuffer);
                }
                break;
            case CALLNC:
                if (!testFlag(FLAG_CARRY)) {
                    call(addressBuffer);
                }
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
            case RST_08H:
                jumpToInterrupt(0x0008);
                break;
            case RST_28H:
                jumpToInterrupt(0x0028);
                break;
            case RST_00H:
                jumpToInterrupt(0x0000);
                break;
            case LDZPGA: // load zero page from A
                mem.poke(0xff00 + (tempRegister & 0xff), A & 0xff);
                // mem.RAM[0xff00+(ac1.val&0xff)] = A&0xff;
                break;
            case FETCH_ZPG: // load zero page from A
                tempRegister = mem.peek(0xff00 + (tempRegister & 0xff));
                // mem.RAM[0xff00+(ac1.val&0xff)] = A&0xff;
                break;
            case ADDSPNN:

                wrk = SP + convertSignedByte(tempRegister & 0xff);

                if (((SP ^ convertSignedByte(tempRegister & 0xff) ^ wrk) & 0x10) > 0)
                    setFlag(FLAG_HALFCARRY);
                else
                    unsetFlag(FLAG_HALFCARRY);

                if (((SP ^ convertSignedByte(tempRegister & 0xff) ^ wrk) & 0x100) > 0)
                    setFlag(FLAG_CARRY);
                else
                    unsetFlag(FLAG_CARRY);

                unsetFlag(FLAG_ZERO);
                unsetFlag(FLAG_ADDSUB);

                SP = wrk;

                break;
            case DI:
                disableInterrupts();
                break;
            case EI:
                enableInterrupts();
                break;
            case LDHLSPN:
                int signedByte = convertSignedByte(tempRegister & 0xff);
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
            case PREFIX_CB:
                CPUPrefixInstructions.processPrefixCommand(this, tempRegister);
                break;
            default:
                System.out.println("Unsupported micro op: " + op.name());
        }
    }


    public void call(int addr) {
        pushW(PC);
        PC = addr;
    }

    public void ret() {
        PC = popW();
    }

    public int popW() {
        int lb = mem.peek((SP++) & 0xffff);
        int hb = mem.peek((SP++) & 0xffff);
        return combineBytes(hb, lb);
    }

    // Combine two bytes into one 16 bit value.
    public int combineBytes(int h, int l) {
        return ((h & 0xff) << 8) | (l & 0xff);
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
        }
    }

    public void jumpToInterrupt(int addr) {
        pushW(PC);
        PC = addr;
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

        PC = (PC + move) & 0xffff;

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

    public int getHighByte(int val) {
        return (val >> 8) & 0xff;
    }

    public int getLowByte(int val) {
        return val & 0xff;
    }

    public int getBC() {
        return combineBytes(B, C);
    }

    public void setBC(int val) {
        val = 0xffff & val;
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

    public void enableInterrupts() {
        pendingEnableInterrupt = 1;
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
        return mem.peek(PC++) & 0xff;
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
}
