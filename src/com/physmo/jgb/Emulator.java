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

	boolean useBios = false;

	//private static final String gameFileName = "resource/tetris.gb";
	//private static final String gameFileName = "resource/mario.gb";
	//private static final String gameFileName = "resource/drmario.gb";
	//private static final String gameFileName = "resource/othello.gb";
	//private static final String gameFileName = "resource/spaceinvaders.gb"; //
	//private static final String gameFileName = "resource/klax.gb";
	// private static final String gameFileName = "resource/tennis.gb";
	// private static final String gameFileName = "resource/bghost.gb";
	// private static final String gameFileName = "resource/motocross.gb";
	// private static final String gameFileName = "resource/asteroids.gb";
	 // private static final String gameFileName = "resource/bowling.gb";
	//private static final String gameFileName = "resource/loderunner.gb";
	//private static final String gameFileName = "resource/pipedream.gb";
	//private static final String gameFileName = "resource/alleyway.gb"; // control problem.

	// private static final String gameFileName = "resource/tesserae.gb";
	// private static final String gameFileName = "resource/serpent.gb";

	//private static final String gameFileName = "resource/batman.gb";
	private static final String gameFileName = "resource/rtype.gb";
	//private static final String gameFileName = "resource/pinballfantasies.gb";
	//private static final String gameFileName = "resource/tetrisattack.gb";
	// private static final String gameFileName = "resource/qbert.gb";
	//private static final String gameFileName = "resource/pacman.gb";
	// private static final String gameFileName = "resource/nemesis2.gb";
	//private static final String gameFileName = "resource/mario2.gb";
	//private static final String gameFileName = "resource/bomberman.gb";
	//private static final String gameFileName = "resource/zelda.gb";
	//private static final String gameFileName = "resource/garfield.gb";
	//private static final String gameFileName = "resource/lemmings.gb";
	//private static final String gameFileName = "resource/hook.gb";
	//private static final String gameFileName = "resource/xenon2.gb";
	
	// BAD PALETTES
	//private static final String gameFileName = "resource/gargoyle.gb";

	// GAMEBOY COLOR
	//private static final String gameFileName = "resource/tetrisdx.gbc";
	//private static final String gameFileName = "resource/mariodx.gbc"; // CART TYPE 0x1B
	//private static final String gameFileName = "resource/vrally.gbc";
	//private static final String gameFileName = "resource/internationalrally.gbc";
	//private static final String gameFileName = "resource/harvestmoon3.gbc";
	//private static final String gameFileName = "resource/rtypedx.gbc"; // 1B  MBC5+RAM+BATTERY
	//private static final String gameFileName = "resource/turok.gbc"; // cart type 0x19
	//private static final String gameFileName = "resource/dk.gbc"; // 0x1b
	//private static final String gameFileName = "resource/zelda.gbc"; //  0x1b
	//private static final String gameFileName = "resource/warioland3.gbc"; // 1B  MBC5+RAM+BATTERY
	//private static final String gameFileName = "resource/mortalkombat.gbc";
	//private static final String gameFileName = "resource/microsoftentertainmentpack.gbc";
	//private static final String gameFileName = "resource/yodastories.gbc"; //
	//private static final String gameFileName = "resource/f18thunderstrike.gbc";
	//private static final String gameFileName = "resource/F1WorldGrandPrixII.gbc";
	//private static final String gameFileName = "resource/StreetFighterAlpha.gbc";
	//private static final String gameFileName = "resource/pokemoncrystal.gbc";
	
	// NON WORKING GAMES
	//private static final String gameFileName = "resource/oracle.gbc"; // 
	//private static final String gameFileName = "resource/pokemon_blue.gb"; // 13
	//private static final String gameFileName = "resource/tombraider.gbc"; // 0x1b
	//private static final String gameFileName = "resource/rayman.gbc"; // 0x19 - not working
	//private static final String gameFileName = "resource/aladdin.gbc"; // 19 - not working
	//private static final String gameFileName = "resource/tony.gbc"; // 19 not working
	//private static final String gameFileName = "resource/startrek.gb";
	//private static final String gameFileName = "resource/spot.gb";
	//private static final String gameFileName = "resource/bombjack.gb";
	//private static final String gameFileName = "resource/centipede.gb";
	//private static final String gameFileName = "resource/arcade1.gb";
	//private static final String gameFileName = "resource/aladdin.gb";
	//private static final String gameFileName = "resource/megamanextreme.gbc";
	

	// private static final String gameFileName = "resource/cpu_instrs.gb";
	// private static final String gameFileName = "resource/tests/opus5.gb";
	// private static final String gameFileName = "resource/tests/01-special.gb"; // PASS
	// private static final String gameFileName = "resource/tests/02-interrupts.gb"; // FAIL
	// private static final String gameFileName = "resource/tests/03-op sp,hl.gb"; // PASS
	// private static final String gameFileName = "resource/tests/04-op r,imm.gb"; // PASS
	// private static final String gameFileName = "resource/tests/05-op rp.gb"; // PASS
	// private static final String gameFileName = "resource/tests/06-ld r,r.gb"; // PASS
	// private static final String gameFileName = "resource/tests/07-jr,jp,call,ret,rst.gb"; // PASS
	// private static final String gameFileName = "resource/tests/08-misc instrs.gb"; // PASS
	// private static final String gameFileName = "resource/tests/09-op r,r.gb"; // PASS
	// private static final String gameFileName = "resource/tests/10-bit ops.gb"; // PASS
	// private static final String gameFileName = "resource/tests/11-op a,(hl).gb"; // PASS

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
		//Utils.ReadFileBytesToMemoryLocation("resource/gbc_bios.bin", mem.BIOS, 0);
		
		Utils.ReadFileBytesToMemoryLocation(gameFileName, mem.CARTRIDGE, 0);

		mem.init(); // Need to load the cartridge before initing memory.
		
		// Initial state.
		setInitialState();
		cpu.A = 0x11; // GBC
	}

	public void run() {
		System.out.println("Cartridge type: " + Utils.toHex2(HEADER.getMemoryBankControllerType(cpu))+"  " + HEADER.getMemoryBankControllerName(cpu));
		if (useBios) {
			cpu.mem.biosActive = true;
			cpu.PC = 0x0000;
		} else {
			cpu.mem.biosActive = false;
			cpu.PC = 0x0100;
		}

		boolean run = true;
		//int tick = 0;
		while (run) {
			tick();
			gpu.tick(cpu, basicDisplay, 10);
		}
	}

	public void tick() {
		cpu.tick();
		timer.tick(1); // random number.
		input.tick(basicDisplay);
		
		//degradeMemory();
	}

	public void keyboardStub() {
		// z=90 x=88 up:38 down:40 left:37 right:39
		// space:32 return:10
		if (basicDisplay.getKeyState()[65] > 0) {

		}
	}
	
	public void degradeMemory() {

		// Vram
		if (Math.random()<0.0002) glitchArea(0x8000, 0x9fff);
		
		// OAM
		if (Math.random()<0.002) glitchArea(0xFE00, 0xFE9F);
		
		// High RAM (HRAM)	
		//if (Math.random()<0.02) glitchArea(0xFF80, 0xFFFE);
		
		// work ram bank 0 
		//if (Math.random()<0.0002) glitchArea(0xC000, 0xCFFF);
	}
	
	public void glitchArea(int start, int end) {
		int addr = start + (int)(Math.random()*(end-start));
		int val = mem.peek(addr);
		val += (int)((Math.random()-0.5)*3);
		val = val & 0xff;
		mem.poke(addr,val);
	}
	
	
	
	// Set initial state as if the BIOS / Bootstrap had been run.
	public void setInitialState() {
		cpu.setAF(0x01B0);
		cpu.setBC(0x0013);
		cpu.setDE(0x00D8);
		cpu.setHL(0x014D);
		cpu.SP=0xFFFE;
	   
		mem.poke(0xFF05,0x00); //   ; TIMA
		mem.poke(0xFF06,0x00); //   ; TMA
		mem.poke(0xFF07,0x00); //   ; TAC
		mem.poke(0xFF10,0x80); //   ; NR10
		mem.poke(0xFF11,0xBF); //   ; NR11
		mem.poke(0xFF12,0xF3); //   ; NR12
		mem.poke(0xFF14,0xBF); //   ; NR14
		mem.poke(0xFF16,0x3F); //   ; NR21
		mem.poke(0xFF17,0x00); //   ; NR22
		mem.poke(0xFF19,0xBF); //   ; NR24
		mem.poke(0xFF1A,0x7F); //   ; NR30
		mem.poke(0xFF1B,0xFF); // v   ; NR31
		mem.poke(0xFF1C,0x9F); // v   ; NR32
		mem.poke(0xFF1E,0xBF); //   ; NR33
		mem.poke(0xFF20,0xFF); //   ; NR41
		mem.poke(0xFF21,0x00); //   ; NR42
		mem.poke(0xFF22,0x00); //   ; NR43
		mem.poke(0xFF23,0xBF); //   ; NR30
		mem.poke(0xFF24,0x77); //   ; NR50
		mem.poke(0xFF25,0xF3); //   ; NR51
		mem.poke(0xFF26,0xF1); //-GB, $F0-SGB ; NR52
		mem.poke(0xFF40,0x91); //   ; LCDC
		mem.poke(0xFF42,0x00); //   ; SCY
		mem.poke(0xFF43,0x00); //   ; SCX
		mem.poke(0xFF45,0x00); //   ; LYC
		mem.poke(0xFF47,0xFC); //   ; BGP
		mem.poke(0xFF48,0xFF); //   ; OBP0
		mem.poke(0xFF49,0xFF); //   ; OBP1
		mem.poke(0xFF4A,0x00); //   ; WY
		mem.poke(0xFF4B,0x00); //   ; WX
		mem.poke(0xFFFF,0x00); //   ; IE
	}
}
