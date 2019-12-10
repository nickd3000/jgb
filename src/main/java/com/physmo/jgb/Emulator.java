package com.physmo.jgb;

import com.physmo.minvio.BasicDisplay;
import com.physmo.minvio.BasicDisplayAwt;

public class Emulator {

	private String biosPath = "/Users/nick/emulatorsupportfiles/gb/bios/";
	private String romPath = "/Users/nick/emulatorsupportfiles/gb/roms/";

	private int displayScale = 3;
	private CPU cpu = null;
	private GPU gpu = null;
	private MEM mem = null;
	private INPUT input = null;
	private TIMER timer = null;
	private Gui gui;

	private static BasicDisplay basicDisplay = null;

	private boolean useBios = false;

	public static void main(String[] args) {
		Debug.checkInstructionDefs();

		Emulator emulator = new Emulator();
		emulator.run();
	}

	private Emulator() {
		cpu = new CPU();
		gpu = new GPU(displayScale);
		mem = new MEM(cpu);
		input = new INPUT(cpu);
		timer = new TIMER(cpu);
		gui = new Gui(this);

		cpu.attachHardware(mem, input, gpu);
		basicDisplay = new BasicDisplayAwt(160*displayScale,144*displayScale);
		basicDisplay.setTitle("JGB");

		Utils.ReadFileBytesToMemoryLocation(biosPath+"dmg_boot.bin", mem.BIOS, 0);
		//Utils.ReadFileBytesToMemoryLocation("resource/gbc_bios.bin", mem.BIOS, 0);

		String gameFileName = DebugRomChoser.getDebugRomName();

		loadCart(romPath + gameFileName );

		reset();
	}

	public void loadCart(String path) {
		Utils.ReadFileBytesToMemoryLocation(path, mem.CARTRIDGE, 0);

		mem.init(); // Need to load the cartridge before initing memory.

	}

	public void reset() {

		if (useBios) {
			cpu.mem.biosActive = true;
			cpu.PC = 0x0000;
		} else {
			cpu.mem.biosActive = false;
			cpu.PC = 0x0100;
		}

		// Initial state.
		setInitialState();
		cpu.A = 0x11; // GBC

	}

	private void run() {
		System.out.println("Cartridge type: " + Utils.toHex2(CartHeader.getMemoryBankControllerType(cpu))+"  " + CartHeader.getMemoryBankControllerName(cpu));
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

	private void tick() {
		cpu.tick();
		timer.tick(1); // random number.
		input.tick(basicDisplay);
		gui.tick(basicDisplay);
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
		if (Math.random()<0.002) glitchArea(0x8000, 0x9fff);
		
		// OAM
		if (Math.random()<0.002) glitchArea(0xFE00, 0xFE9F);
		
		// High RAM (HRAM)	
		//if (Math.random()<0.02) glitchArea(0xFF80, 0xFFFE);
		
		// work ram bank 0 
		if (Math.random()<0.002) glitchArea(0xC000, 0xCFFF);
	}
	
	private void glitchArea(int start, int end) {
		int addr = start + (int)(Math.random()*(end-start));
		int val = mem.peek(addr);
		val += (int)((Math.random()-0.5)*3);
		val = val & 0xff;
		mem.poke(addr,val);
	}
	
	
	
	// Set initial state as if the BIOS / Bootstrap had been run.
	private void setInitialState() {
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
