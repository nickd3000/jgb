package com.physmo.jgb;

public class Emulator {

	CPU cpu = null;
	MEM mem = null;
	
	public static void main(String[] args) {
		
		Emulator emulator = new Emulator();
		
		emulator.run();
	}
	
	public Emulator() {
		cpu = new CPU();
		mem = new MEM(cpu);
		cpu.attachHardware(mem);
		
		Utils.ReadFileBytesToMemoryLocation("resource/dmg_boot.bin", mem.ROM, 0);
		Utils.ReadFileBytesToMemoryLocation("resource/tetris.gb", mem.CARTRIDGE, 0);
		
		//Debug.printMem(cpu, 0x104, 64);
	}
	
	public void run() {
		
		for (int i=0;i<6400000;i++) {
			tick();
		}
	}
	
	public void tick() {
		cpu.tick();
	}
	
}
