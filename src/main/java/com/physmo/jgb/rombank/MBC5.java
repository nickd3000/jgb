package com.physmo.jgb.rombank;

import com.physmo.jgb.CPU;
import com.physmo.jgb.MEM;

public class MBC5 implements ROMBank {

    public CPU cpu;
    // public MEM mem;
    int currentRomBank = 0;
    int currentRamBank = 0;
    int romBanking = 0;
    boolean enableRam = false;
    boolean m_MBC1 = true;
    boolean m_MBC2 = false;
    int[] RAM_BANKS = new int[0xffff];
    private final MEM mem;

    public MBC5(CPU cpu) {
        this.cpu = cpu;
        mem = cpu.getMem();

        // this.mem = cpu.mem;
    }

    @Override
    public void poke(int address, int data) {
        int location_hi = ((address & 0xF000) >> 12);
        switch (location_hi) {
            case 0x0:
            case 0x1:
                enableRam = (data & 0x0A) == 0x0A;
                break;
            case 0x2:
                currentRomBank &= 0xFF00;
                currentRomBank |= data & 0xff;
                break;
            case 0x3:
                currentRomBank &= 0x00FF;
                currentRomBank |= ((data & 0x1) << 8);
                break;
            case 0x4:
            case 0x5:
                currentRamBank = (data & 0x0F);
                break;
            case 0x6:
            case 0x7:
            case 0x8:
            case 0x9:
                break;
            case 0xA:
            case 0xB:
                int newAddress = address - 0xA000;
                mem.CART_RAM_BANKS[newAddress + (currentRamBank * 0x2000)] = data & 0xff;
                break;
//        case 0xC:
////            location &= 0x0FFF;
////            gb->ram[location] = data;
//            break;
//        case 0xD:
////            location &= 0x0FFF;
////            gb->selected_ram[location] = data;
//            break;
        }


//		if (address <= 0x7FFF) {
//			handleBankChange(address, data);
//		}
//
//		else if ((address >= 0xA000) && (address <= 0xBFFF)) {
//			if (enableRam) {
//				int newAddress = address - 0xA000;
//				cpu.mem.RAM_BANKS[newAddress + (currentRamBank * 0x2000)] = data;
//			}
//		}
    }

    @Override
    public int peek(int address) {
        // 0x4000 - 0x7FFF (16,384 bytes) Cartridge ROM Bank n
        // 0xA000 - 0xBFFF (8,192 bytes) External RAM
        //int newAddress = addr - 0x4000;
        // return cpu.mem.CARTRIDGE[newAddress + (currentRomBank * 0x4000)];

        // Are we reading from the SWITCHABLE ROM cartridge memory bank?
        if ((address >= 0x4000) && (address <= 0x7FFF)) {
            int newAddress = address - 0x4000;
            return mem.CARTRIDGE[newAddress + (currentRomBank * 0x4000)];
        }

        // Are we reading from the cartridge RAM memory bank?
        if ((address >= 0xA000) && (address <= 0xBFFF)) {
            int newAddress = address - 0xA000;
            return mem.CART_RAM_BANKS[newAddress + (currentRamBank * 0x2000)];
        }

        // else return memory
        return mem.CARTRIDGE[address];

    }

    public void handleBankChange(int address, int data) {
        // do RAM enabling
        if (address < 0x2000) {
            if (m_MBC1 || m_MBC2) {
                DoRAMBankEnable(address, data);
            }
        }

        // do ROM bank change
        else if ((address >= 0x2000) && (address < 0x4000)) {
            if (m_MBC1 || m_MBC2) {
                DoChangeLoROMBank(data);
            }
        }

        // do ROM or RAM bank change
        else if ((address >= 0x4000) && (address < 0x6000)) {
            // there is no rambank in mbc2 so always use rambank 0
            if (m_MBC1) {
                if (romBanking == 1) {
                    DoChangeHiRomBank(data);
                } else {
                    DoRAMBankChange(data);
                }
            }

        }

        // this will change whether we are doing ROM banking
        // or RAM banking with the above if statement
        else if ((address >= 0x6000) && (address < 0x8000)) {
            if (m_MBC1)
                DoChangeROMRAMMode(data);
        }

        //System.out.println("Active ROM Bank: "+currentRomBank);
    }

    void DoRAMBankChange(int data) {
        currentRamBank = data & 0x3;
    }

    void DoChangeLoROMBank(int data) {
        if (m_MBC2) {
            currentRomBank = data & 0xF;
            if (currentRomBank == 0)
                currentRomBank++;
            return;
        }

        int lower5 = data & 0x1F;
        currentRomBank &= 0xE0; // turn off the lower 5
        currentRomBank |= lower5;
        if (currentRomBank == 0)
            currentRomBank++;
    }

    void DoChangeHiRomBank(int data) {
        // turn off the upper 3 bits of the current rom
        currentRomBank &= 0x1F;

        // turn off the lower 5 bits of the data
        data &= 0xE0;
        currentRomBank |= data;
        if (currentRomBank == 0)
            currentRomBank++;
    }

    void DoChangeROMRAMMode(int data) {
        int newData = data & 0x1;
        romBanking = (newData == 0) ? 1 : 0;
        if (romBanking > 0)
            currentRamBank = 0;
    }

    void DoRAMBankEnable(int address, int data) {
        if (m_MBC2) {
            if (TestBit(address, 4) == 1)
                return;
        }

        int testData = data & 0xF;
        if (testData == 0xA)
            enableRam = true;
        else if (testData == 0x0)
            enableRam = false;
    }

    public int TestBit(int val, int bit) {
        if ((val & (1 << bit)) > 0)
            return 1;
        return 0;
    }

}
