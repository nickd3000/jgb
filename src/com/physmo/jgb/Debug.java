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
	
	// Print section of memory as if it is being peeked.
	public static void printMem(CPU cpu, int start, int length) {
		String line = "";
		int end = start+length;
		int addr = start;
		
		while (addr<end) line+=" 0x"+Utils.toHex2(cpu.mem.peek(addr++));
		
		System.out.println(line);
	}
	
}
