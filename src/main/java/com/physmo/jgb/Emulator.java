package com.physmo.jgb;

import com.physmo.minvio.BasicDisplay;
import com.physmo.minvio.BasicDisplayAwt;

// TODO: Pair this class down to the essentials.
public class Emulator {

    public static final int GPU_CYCLES_PER_TICK = 10;
    private static BasicDisplay basicDisplay = null;
    private String biosPath = "/Users/nick/dev/emulatorsupportfiles/gb/bios/";
    private String romPath = "/Users/nick/dev/emulatorsupportfiles/gb/roms/";
    private int displayScale = 3;
    private CPU cpu = null;
    private GPU gpu = null;
    private MEM mem = null;
    private INPUT input = null;
    private TIMER timer = null;
    private Gui gui;
    private boolean useBios = false;
    private boolean loadRomOnStartup = true;

    private Emulator() {
        cpu = new CPU();
        gpu = new GPU(displayScale);
        mem = new MEM(cpu);
        input = new INPUT(cpu);
        timer = new TIMER(cpu);
        gui = new Gui(this);

        cpu.attachHardware(mem, input, gpu);
        basicDisplay = new BasicDisplayAwt(160 * displayScale, 144 * displayScale);
        basicDisplay.setTitle("JGB - Java GB Emulator");

        if (useBios) {
            Utils.ReadFileBytesToMemoryLocation(biosPath + "dmg_boot.bin", mem.BIOS, 0);
        }
        //Utils.ReadFileBytesToMemoryLocation("resource/gbc_bios.bin", mem.BIOS, 0);

        if (loadRomOnStartup) {
            String gameFileName = DebugRomChoser.getDebugRomName();
            loadCart(romPath + gameFileName);
        }

        reset();
    }

    public static void main(String[] args) {
        Debug.checkInstructionDefs();

        Emulator emulator = new Emulator();
        emulator.run();
    }

    // TODO: should we init all state here?
    public void loadCart(String path) {

        reset();

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
        StartupState.setStartupState(cpu, mem);
        cpu.A = 0x11; // GBC

    }

    private void run() {
        System.out.println("Cartridge type: " + Utils.toHex2(CartHeader.getMemoryBankControllerType(cpu)) + "  " + CartHeader.getMemoryBankControllerName(cpu));
        if (useBios) {
            cpu.mem.biosActive = true;
            cpu.PC = 0x0000;
        } else {
            cpu.mem.biosActive = false;
            cpu.PC = 0x0100;
        }

        boolean run = true;

        while (run) {
            tick();
            gpu.tick(cpu, basicDisplay, GPU_CYCLES_PER_TICK);
        }
    }

    private void tick() {
        cpu.tick();
        timer.tick(1); // random number (what?).
        input.tick(basicDisplay);
        gui.tick(basicDisplay);
        //MemoryDegrader.degradeMemory(mem);
    }

    public void keyboardStub() {
        // z=90 x=88 up:38 down:40 left:37 right:39
        // space:32 return:10
        if (basicDisplay.getKeyState()[65] > 0) {

        }
    }




}
