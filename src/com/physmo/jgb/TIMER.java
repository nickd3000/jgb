package com.physmo.jgb;

public class TIMER {

	public static int TIMA = 0xFF05;
	public static int TMA = 0xFF06;
	public static int TMC = 0xFF07;

	CPU cpu = null;
	int timerCounter = 1024;

	int dividerRegister = 0;
	int dividerCounter = 0;

	public TIMER(CPU cpu) {
		this.cpu = cpu;
	}

	public boolean isClockEnabled() {
		int iTMC = cpu.mem.RAM[TMC];
		
		if ((iTMC & (1 << 2)) > 0)
			return true;
		
		return false;
		// return TestBit(ReadMemory(TMC),2)?true:false ;
	}

	public int getClockFreq() {
		int val = cpu.mem.RAM[TMC];
		return val & 0x3;
	}

	public void SetClockFreq() {
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

		//System.out.println("hello");

		// the clock must be enabled to update the clock
		if (isClockEnabled()) {
			timerCounter -= cycles;

			// enough cpu clock cycles have happened to update the timer
			if (timerCounter <= 0) {
				// reset m_TimerTracer to the correct value
				SetClockFreq();

				// timer about to overflow
				if (cpu.mem.RAM[TIMA] == 255) {
					cpu.mem.RAM[TIMA] = cpu.mem.RAM[TMA];
					cpu.requestInterrupt(CPU.INT_TIMER);
					//System.out.println("Timer running TMA:"+cpu.mem.RAM[TMA]);
				} else {
					cpu.mem.RAM[TIMA]++;
				}
			}
		}
	}


	public void doDividerRegister(int cycles) {
		dividerCounter += cycles;
		if (dividerCounter >= 255) {
			dividerCounter = 0;
			cpu.mem.RAM[0xFF04]=(cpu.mem.RAM[0xFF04]+1)&0xff;
		}
	}
}
