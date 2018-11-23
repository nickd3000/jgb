package com.physmo.jgb;

import com.physmo.toolbox.BasicDisplay;

public class Emulator {

	CPU cpu = null;
	MEM mem = null;
	static BasicDisplay basicDisplay = null;
	
	//private static final String gameFileName = "resource/tetris.gb";
	//private static final String gameFileName = "resource/othello.gb";
	//private static final String gameFileName = "resource/spaceinvaders.gb";
	//private static final String gameFileName = "resource/klax.gb";
	//private static final String gameFileName = "resource/mario.gb";
	//private static final String gameFileName = "resource/bombjack.gb";
	//private static final String gameFileName = "resource/centipede.gb";
	private static final String gameFileName = "resource/drmario.gb";
	//private static final String gameFileName = "resource/tennis.gb";
	//private static final String gameFileName = "resource/bghost.gb";
	//private static final String gameFileName = "resource/motocross.gb";
	
	
	public static void main(String[] args) {
		
		Emulator emulator = new Emulator();
		
		emulator.run();
	}
	
	public Emulator() {
		cpu = new CPU();
		mem = new MEM(cpu);
		cpu.attachHardware(mem);
		basicDisplay = new BasicDisplay(320*2, 240*2);
		
		Utils.ReadFileBytesToMemoryLocation("resource/dmg_boot.bin", mem.ROM, 0);
		Utils.ReadFileBytesToMemoryLocation(gameFileName, mem.CARTRIDGE, 0);
		
		//Debug.printMem(cpu, 0x104, 64);
	}
	
	public void run() {
		cpu.mem.biosActive=true;
		//cpu.PC=0x0100;
		cpu.PC=0x0000;
		boolean run = true;
		int tick=0;
		while(run) {
			tick();
			if ((tick++)%500==0) DisplayStub.render(cpu, basicDisplay);
		}
	}
	
	public void tick() {
		cpu.tick();
	}
	
}
