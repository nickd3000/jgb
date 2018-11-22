package com.physmo.jgb;

public class Debug {
	
	public static String dissasemble(CPU cpu, int addr) {
		String str = "> ";
		int instr = cpu.mem.peek(addr);
		str += "0x"+Utils.toHex4(addr); // Address
		
		InstructionDefinition def = InstructionDefinition.getEnumFromId(instr);
		str+= "   "+def.toString();
		
		return str;
	}
	


	  
	public static String getFlags(CPU cpu) {
		String str = "";
		
		str+=cpu.testFlag(CPU.FLAG_ZERO)?"zf ":" - ";
		str+=cpu.testFlag(CPU.FLAG_ADDSUB)?" n":" - ";
		str+=cpu.testFlag(CPU.FLAG_HALFCARRY)?" hc ":"  - ";
		str+=cpu.testFlag(CPU.FLAG_CARRY)?" c ":" - ";
		
		return str;
	}
	
	/*
	 * 	int A,B,C,D,E,H,L;
		int PC; // Program counter
		int SP; // Stack pointer
		int FL;
	 */
	public static String getRegisters(CPU cpu) {
		String str = "";
		
		str+="A:"+Utils.toHex2(cpu.A);
		str+=" B:"+Utils.toHex2(cpu.B);
		str+=" C:"+Utils.toHex2(cpu.C);
		str+=" D:"+Utils.toHex2(cpu.D);
		str+=" E:"+Utils.toHex2(cpu.E);
		str+=" H:"+Utils.toHex2(cpu.H);
		str+=" L:"+Utils.toHex2(cpu.L);
		str+=" PC:"+Utils.toHex4(cpu.PC);
		str+=" SP:"+Utils.toHex4(cpu.SP);
		str+=" FL:"+Utils.toHex2(cpu.FL);
		
		
		return str;
	}
	
	public static String getStackData(CPU cpu, int numItems) {
		int stackSize = ((0xFFFE-cpu.SP)/2);
		String str = "  STK:("+stackSize+")";
		int ptr = cpu.SP;
		if (ptr<10) return "invalid stack";
		if (numItems>stackSize) numItems=stackSize;
		int b1,b2;
		for (int i=0;i<numItems;i++) {
			//if (ptr<=0xff-1)
			b1=cpu.mem.RAM[++ptr];
			b2=cpu.mem.RAM[++ptr];
			str+=" "+Utils.toHex4(cpu.combineBytes(b2,b1));
		}
		return str;
	}
	
	public static String getInterruptFlags(CPU cpu) {
		int if1 = cpu.mem.RAM[0xFFFF];
		int if2 = cpu.mem.RAM[0xFF0F];
		
		String str = "INT:"+if1+"|"+if2+" ie:"+cpu.interruptEnabled;
		return str;
	}
	
	// Print section of memory as if it is being peeked.
	public static void printMem(CPU cpu, int start, int length) {
		String line = "";
		int end = start+length;
		int addr = start;
		
		while (addr<end) line+=" 0x"+Utils.toHex2(cpu.mem.peek(addr++));
		
		System.out.println(line);
	}
	
	
	public static void checkInstructionDefs() {
		for (InstructionDefinition id : InstructionDefinition.values()) {
			ADDRMODE am1 = id.getAddressMode1();
			ADDRMODE am2 = id.getAddressMode2();
			
			int total = getAddressModeBytes(am1)+getAddressModeBytes(am2);
			int numBytes = id.getNumBytes();
			
		}
		
	}
	
	public static int getAddressModeBytes(ADDRMODE a) {
		switch (a) {
		
		case __nnnn: return 2;
		case nnnn: return 2;
		case nn: return 1;
		
		default: return 0;
		}
		
	}
}
