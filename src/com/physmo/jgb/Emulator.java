package com.physmo.jgb;

import com.physmo.toolbox.BasicDisplay;

public class Emulator {

	int displayScale = 3;
	CPU cpu = null;
	GPU gpu = null;
	MEM mem = null;
	INPUT input = null;
	TIMER timer = null;

	static BasicDisplay basicDisplay = null;

	boolean useBios = true;

	//private static final String gameFileName = "resource/tetris.gb";
	//private static final String gameFileName = "resource/drmario.gb";
	// private static final String gameFileName = "resource/othello.gb";
	// private static final String gameFileName = "resource/spaceinvaders.gb"; //
	// Uses timer
	// private static final String gameFileName = "resource/klax.gb"; // Uses timer
	// private static final String gameFileName = "resource/tennis.gb";
	// private static final String gameFileName = "resource/bghost.gb";
	// private static final String gameFileName = "resource/motocross.gb";
	// private static final String gameFileName = "resource/asteroids.gb";
	 // private static final String gameFileName = "resource/bowling.gb";
	// private static final String gameFileName = "resource/loderunner.gb";
	//private static final String gameFileName = "resource/pipedream.gb";
	//private static final String gameFileName = "resource/alleyway.gb"; // control problem.

	// private static final String gameFileName = "resource/tesserae.gb";
	// private static final String gameFileName = "resource/serpent.gb";

	//private static final String gameFileName = "resource/batman.gb";
	
	// NON WORKING GAMES
	////private static final String gameFileName = "resource/spot.gb";
	//private static final String gameFileName = "resource/bombjack.gb";
	//private static final String gameFileName = "resource/centipede.gb";
	//private static final String gameFileName = "resource/arcade1.gb";
	//private static final String gameFileName = "resource/aladdin.gb";

	//
	//private static final String gameFileName = "resource/xenon2.gb";
	// private static final String gameFileName = "resource/mario.gb";
	// private static final String gameFileName = "resource/qbert.gb";
	//private static final String gameFileName = "resource/pacman.gb";
	// private static final String gameFileName = "resource/pokemon_blue.gb";
	 private static final String gameFileName = "resource/nemesis2.gb";
	//private static final String gameFileName = "resource/mario2.gb";
	// private static final String gameFileName = "resource/bomberman.gb";
	// private static final String gameFileName = "resource/gargoyle.gb";
	//private static final String gameFileName = "resource/zelda.gb";
	//private static final String gameFileName = "resource/garfield.gb";
	 //private static final String gameFileName = "resource/lemmings.gb";
	//private static final String gameFileName = "resource/hook.gb";

	// private static final String gameFileName = "resource/cpu_instrs.gb";
	// private static final String gameFileName = "resource/tests/opus5.gb";
	// private static final String gameFileName = "resource/tests/01-special.gb"; //
	// PASS
	// private static final String gameFileName = "resource/tests/02-interrupts.gb";
	// // FAIL
	// private static final String gameFileName = "resource/tests/03-op sp,hl.gb";
	// // PASS
	// private static final String gameFileName = "resource/tests/04-op r,imm.gb";
	// // PASS
	// private static final String gameFileName = "resource/tests/05-op rp.gb"; //
	// PASS
	// private static final String gameFileName = "resource/tests/06-ld r,r.gb"; //
	// PASS
	// private static final String gameFileName =
	// "resource/tests/07-jr,jp,call,ret,rst.gb"; // PASS
	// private static final String gameFileName = "resource/tests/08-misc
	// instrs.gb"; // PASS
	// private static final String gameFileName = "resource/tests/09-op r,r.gb"; //
	// PASS
	// private static final String gameFileName = "resource/tests/10-bit ops.gb"; //
	// PASS
	// private static final String gameFileName = "resource/tests/11-op a,(hl).gb";
	// // PASS

	public static void main(String[] args) {
		Debug.checkInstructionDefs();

		Emulator emulator = new Emulator();
		emulator.run();
	}

	public Emulator() {
		cpu = new CPU();
		gpu = new GPU(displayScale);
		mem = new MEM(cpu);
		input = new INPUT(cpu);
		timer = new TIMER(cpu);

		cpu.attachHardware(mem, input, gpu);
		basicDisplay = new BasicDisplay(160*displayScale,144*displayScale);
		basicDisplay.setTitle("JGB");

		Utils.ReadFileBytesToMemoryLocation("resource/dmg_boot.bin", mem.BIOS, 0);
		Utils.ReadFileBytesToMemoryLocation(gameFileName, mem.CARTRIDGE, 0);

		mem.init(); // Need to load the cartridge before initing memory.
	}

	public void run() {
		System.out.println("Cartridge type: " + HEADER.getMemoryBankControllerName(cpu));
		if (useBios) {
			cpu.mem.biosActive = true;
			cpu.PC = 0x0000;
		} else {
			cpu.mem.biosActive = false;
			cpu.PC = 0x0100;
		}

		boolean run = true;
		int tick = 0;
		while (run) {
			tick();
			if ((tick++) % 100 == 0) {
				gpu.tick(cpu, basicDisplay, 7);
			}

		}
	}

	public void tick() {
		cpu.tick();
		timer.tick(5); // random number.
		input.tick(basicDisplay);
	}

	public void keyboardStub() {
		// z=90 x=88 up:38 down:40 left:37 right:39
		// space:32 return:10
		if (basicDisplay.getKeyState()[65] > 0) {

		}
	}
}
