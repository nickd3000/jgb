package com.physmo.jgb;

public class TIMER {

    public static int ADDR_0xFF05_TIMA = 0xFF05;
    public static int ADDR_0xFF06_TMA = 0xFF06;
    public static int ADDR_0xFF07_TMC = 0xFF07;

    CPU cpu = null;
    int timerCounter = 1024;

    int dividerRegister = 0;
    int dividerCounter = 0;

    public TIMER(CPU cpu) {
        this.cpu = cpu;
    }

    public boolean isClockEnabled() {
        int iTMC = cpu.mem.RAM[ADDR_0xFF07_TMC];

        return (iTMC & (1 << 2)) > 0;
        // return TestBit(ReadMemory(TMC),2)?true:false ;
    }

    public int getClockFreq() {
        int val = cpu.mem.RAM[ADDR_0xFF07_TMC];
        return val & 0x3;
    }

    public void setClockFreq() {
        int freq = getClockFreq();
        switch (freq) {
            case 0:
                timerCounter = 1024;
                break; // freq 4096
            case 1:
                timerCounter = 16;
                break; // freq 262144
            case 2:
                timerCounter = 64;
                break; // freq 65536
            case 3:
                timerCounter = 256;
                break; // freq 16382
        }
    }

    public void tick(int cycles) {
        doDividerRegister(cycles);

        // the clock must be enabled to update the clock
        if (isClockEnabled()) {
            timerCounter -= cycles;

            // enough cpu clock cycles have happened to update the timer
            if (timerCounter <= 0) {
                // reset m_TimerTracer to the correct value
                setClockFreq();

                // timer about to overflow
                if (cpu.mem.RAM[ADDR_0xFF05_TIMA] > 255) {
                    cpu.mem.RAM[ADDR_0xFF05_TIMA] = cpu.mem.RAM[ADDR_0xFF06_TMA];
                    cpu.requestInterrupt(CPU.INT_TIMER);
                } else {
                    cpu.mem.RAM[ADDR_0xFF05_TIMA]++;
                }
            }
        }
    }


    public void doDividerRegister(int cycles) {
        dividerCounter += cycles;
        if (dividerCounter >= 255) {
            dividerCounter = 0;
            cpu.mem.RAM[0xFF04] = (cpu.mem.RAM[0xFF04] + 1) & 0xff;
        }
    }
}
