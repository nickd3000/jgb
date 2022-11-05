package com.physmo.jgb;

import com.physmo.minvio.BasicDisplay;

import javax.swing.JFileChooser;
import java.io.File;

import static java.awt.event.KeyEvent.VK_L;

// Simple file chooser for loading roms
public class Gui {
    //BasicDisplay bd;
    //Create a file chooser
    final JFileChooser fc = new JFileChooser();
    Emulator emulator;
    int[] keyStatePrevious;

    public Gui(Emulator emulator) {
        this.emulator = emulator;
        keyStatePrevious = new int[1500];
    }

    public void tick(BasicDisplay basicDisplay) {
        //JFileChooser
        int loadKey = VK_L;

        int[] keyState = basicDisplay.getKeyState();


        if (keyState[loadKey] > 0 && keyStatePrevious[loadKey] == 0) {
            System.out.println("key " + keyState.length);

            int returnVal = fc.showOpenDialog(null);
            File file = fc.getSelectedFile();
            emulator.loadCart(file.getAbsolutePath());
            emulator.reset();
        }

        System.arraycopy(keyState, 0, keyStatePrevious, 0, keyState.length);

    }
}
