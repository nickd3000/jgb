package com.physmo.jgb;

// CPU Extended commands.
// Some explanation of instructions - rolls etc
// http://www.devrs.com/gb/files/opcodes.html

public class CPUPrefixInstructions {

    public static void processPrefixCommand(CPU cpu, int instr) {

        int bit = getBitForOperation(instr);
        int bitMask = (1 << bit);
        int value = getValueForOperation(cpu, instr);
        int wrk = 0; // Work value.
        int carry = 0;
        boolean operationSupported = false;
        int carryOut = 0;
        int carryIn = 0;

        if (CPU.displayInstruction)
            System.out.println("Prefix command: " + Utils.toHex2(instr) + "   val:" + value + "   bit:" + bit);

        // RLC - rotate left with carry? (guess)
        // bit cycles over to other side
        // checked - nneded addsub and halfcarry cleared.
        if (instr >= 0x00 && instr <= 0x07) {
//			const carryOut = register&0x80?1:0;
//	        if (carryOut) this.setC(); else this.clearC();
//	        const result = ((register<<1)+carryOut)&0xFF;
//	        if (result) this.clearZ(); else this.setZ();
//	        return result;

            carryOut = ((value & 0x80) > 0) ? 1 : 0;
            if (carryOut > 0)
                cpu.setFlag(CPU.FLAG_CARRY);
            else
                cpu.unsetFlag(CPU.FLAG_CARRY);

            wrk = ((value << 1) + carryOut) & 0xff;
            cpu.handleZeroFlag(wrk);

            cpu.unsetFlag(CPU.FLAG_ADDSUB);
            cpu.unsetFlag(CPU.FLAG_HALFCARRY);

            setValueForOperation(cpu, instr, wrk & 0xff);
            operationSupported = true;
        }

        // RRC
        // checked
        if (instr >= 0x08 && instr <= 0x0F) {
            wrk = getValueForOperation(cpu, instr);

            if ((wrk & 1) == 1) carry = 1;

            if (carry == 1)
                cpu.setFlag(CPU.FLAG_CARRY);
            else
                cpu.unsetFlag(CPU.FLAG_CARRY);

            wrk = wrk >> 1;
            if (carry == 1) wrk |= 0x80;

            cpu.handleZeroFlag(wrk);
            cpu.unsetFlag(CPU.FLAG_ADDSUB);
            cpu.unsetFlag(CPU.FLAG_HALFCARRY);

            setValueForOperation(cpu, instr, wrk & 0xff);
            operationSupported = true;
        }

        // RL shift left?
        // rotate through carry
        // checked
        if (instr >= 0x10 && instr <= 0x17) {
            wrk = getValueForOperation(cpu, instr);
            wrk = wrk << 1;

            if (cpu.testFlag(CPU.FLAG_CARRY))
                wrk |= 1;

            if (wrk > 0xff)
                cpu.setFlag(CPU.FLAG_CARRY);
            else
                cpu.unsetFlag(CPU.FLAG_CARRY);

            wrk &= 0xff;

            cpu.handleZeroFlag(wrk);

            cpu.unsetFlag(CPU.FLAG_ADDSUB);
            cpu.unsetFlag(CPU.FLAG_HALFCARRY);

            setValueForOperation(cpu, instr, wrk);
            operationSupported = true;
        }

        // RR
        // checked
        if (instr >= 0x18 && instr <= 0x1F) {
            wrk = getValueForOperation(cpu, instr);

            if (cpu.testFlag(CPU.FLAG_CARRY))
                wrk |= 0x100;

            if ((wrk & 1) > 0)
                cpu.setFlag(CPU.FLAG_CARRY);
            else
                cpu.unsetFlag(CPU.FLAG_CARRY);

            wrk = wrk >> 1;
            cpu.handleZeroFlag(wrk & 0xff);
            cpu.unsetFlag(CPU.FLAG_ADDSUB);
            cpu.unsetFlag(CPU.FLAG_HALFCARRY);

            setValueForOperation(cpu, instr, wrk);
            operationSupported = true;

        }


        // SLA
        // 27 is failing
        if (instr >= 0x20 && instr <= 0x27) {

            wrk = getValueForOperation(cpu, instr);

            if ((wrk & 0x80) > 0)
                cpu.setFlag(CPU.FLAG_CARRY);
            else
                cpu.unsetFlag(CPU.FLAG_CARRY);

            wrk = wrk << 1;

            cpu.handleZeroFlag(wrk & 0xff);

            cpu.unsetFlag(CPU.FLAG_ADDSUB);
            cpu.unsetFlag(CPU.FLAG_HALFCARRY);

            setValueForOperation(cpu, instr, wrk & 0xff);
            operationSupported = true;
//			System.out.println(">AFTER   :"+Utils.toHex2(getValueForOperation(cpu, instr)));
        }

        // SRA
        //
        if (instr >= 0x28 && instr <= 0x2F) {
            wrk = getValueForOperation(cpu, instr);

            if ((wrk & 1) > 0)
                cpu.setFlag(CPU.FLAG_CARRY);
            else
                cpu.unsetFlag(CPU.FLAG_CARRY);

            if ((wrk & 0x80) > 0) {
                wrk |= 0x100;
            }

            wrk = wrk >> 1;

            cpu.handleZeroFlag(wrk & 0xff);

            setValueForOperation(cpu, instr, wrk & 0xff);
            operationSupported = true;

            cpu.unsetFlag(CPU.FLAG_HALFCARRY);
            cpu.unsetFlag(CPU.FLAG_ADDSUB);
            //this.clearN(); this.clearH();

        }

        // SWAP operation
        if (instr >= 0x30 && instr <= 0x37) {
            int upper = (value >> 4) & 0b0000_1111;
            int lower = (value << 4) & 0b1111_0000;
            value = upper | lower;
            setValueForOperation(cpu, instr, value);

            if (value == 0) {
                cpu.setFlag(CPU.FLAG_ZERO);
            } else {
                cpu.unsetFlag(CPU.FLAG_ZERO);
            }

            //this.clearN(); this.clearH(); this.clearC();
            cpu.unsetFlag(CPU.FLAG_CARRY);
            cpu.unsetFlag(CPU.FLAG_HALFCARRY);
            cpu.unsetFlag(CPU.FLAG_ADDSUB);

            operationSupported = true;
        }


        // SRL
        // checked
        if (instr >= 0x38 && instr <= 0x3F) {
            wrk = getValueForOperation(cpu, instr);

            if ((wrk & 1) > 0)
                cpu.setFlag(CPU.FLAG_CARRY);
            else
                cpu.unsetFlag(CPU.FLAG_CARRY);

            wrk = wrk >> 1;
            cpu.handleZeroFlag(wrk);
            cpu.unsetFlag(CPU.FLAG_ADDSUB);
            cpu.unsetFlag(CPU.FLAG_HALFCARRY);

            setValueForOperation(cpu, instr, wrk);
            operationSupported = true;

        }

        // BIT operation
        if (instr >= 0x40 && instr <= 0x7F) {

            if ((value & bitMask) > 0) {
                cpu.unsetFlag(CPU.FLAG_ZERO);
                // System.out.println("unset zero");
            } else {
                cpu.setFlag(CPU.FLAG_ZERO);
                // System.out.println("set zero");
            }
            cpu.unsetFlag(CPU.FLAG_ADDSUB);
            cpu.setFlag(CPU.FLAG_HALFCARRY);
            operationSupported = true;
        }

        // RES - unset?
        // failing blarg test
        if (instr >= 0x80 && instr <= 0xBF) {
            //System.out.println("RES command");
            int tmp = getValueForOperation(cpu, instr);

            //System.out.println("   INST="+Utils.toHex2(instr)+"  VAL="+Utils.toHex2(tmp)+"  MASK="+Utils.toHex2(bitMask));

            //tmp &= (~(bitMask)&0xff);
            tmp &= 0xff - bitMask;
            //System.out.println("   result:"+Utils.toHex2(tmp));
            //tmp &= ((~(bitMask))&0xff);

            setValueForOperation(cpu, instr, tmp & 0xff);
            // FL &= ~(flag);
            operationSupported = true;
        }

        // SET
        if (instr >= 0xC0 && instr <= 0xFF) {
            // FL |= flag;
            wrk = getValueForOperation(cpu, instr);
            wrk |= bitMask;
            setValueForOperation(cpu, instr, wrk);
            operationSupported = true;
        }

        if (!operationSupported) {
            System.out.println(
                    "Unsupported Prefix command: 0x" + Utils.toHex2(instr) + "   val:" + value + "   bit:" + bit);
            cpu.mem.poke(0xffff + 10, 1);
        }
    }

    // public static void main(String[] args) {
    // testBit(0x64); // 4s;
    // testBit(0x74); // 4s;
    // }

    // public static void testBit(int instr) {
    // System.out.println("testBit: "+instr+" "+getBitForOperation(instr));
    // }

    public static int getBitForOperation(int instr) {
        int bit = (instr % 64) / 8;
        // System.out.println(" > instr="+instr);
        // System.out.println(" > instr % 64="+(instr % 64));
        return bit;
    }

    public static int getValueForOperation(CPU cpu, int instr) {
        int stripped = instr & 0x0f;

        switch (stripped & 7) {
            case 0:
                return cpu.B;
            case 1:
                return cpu.C;
            case 2:
                return cpu.D;
            case 3:
                return cpu.E;
            case 4:
                return cpu.H;
            case 5:
                return cpu.L;
            case 6:
                return cpu.mem.peek(cpu.getHL());
            case 7:
                return cpu.A;
        }

        return 0;
    }

    public static void setValueForOperation(CPU cpu, int instr, int val) {

        int stripped = instr & 0x0f;

        switch (stripped & 7) {
            case 0:
                cpu.B = val & 0xff;
                break;
            case 1:
                cpu.C = val & 0xff;
                break;
            case 2:
                cpu.D = val & 0xff;
                break;
            case 3:
                cpu.E = val & 0xff;
                break;
            case 4:
                cpu.H = val & 0xff;
                break;
            case 5:
                cpu.L = val & 0xff;
                break;
            case 6:
                cpu.mem.poke(cpu.getHL(), val);
                break;
            case 7:
                cpu.A = val & 0xff;
                break;
        }

    }

}
