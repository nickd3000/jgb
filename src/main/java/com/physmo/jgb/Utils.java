package com.physmo.jgb;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Utils {

    // TODO: change this to take an array reference instead of CPU class reference.
    public static void ReadFileBytesToMemoryLocation(String fileName, int[] memoryDevice, int targetLocation) /*throws IOException*/ {
        FileInputStream in = null;

        try {
            in = new FileInputStream(fileName);

            int c;
            int count = 0;


            while ((c = in.read()) != -1) {
//            	if (count>=0x0150 && count<=0x0160) {
//                	System.out.println("LOAD 0x" + Utils.toHex4(count) + "  " + Utils.toHex2(c) );
//                }

                memoryDevice[targetLocation + count++] = c;
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static String toHex2(int v) {
        return String.format("%02X", v);
    }

    public static String toHex4(int v) {
        return String.format("%04X", v);
    }

    // Messy function to add spaces to a string to bring it up to a certain length.
    public static String padToLength(String str, int length) {
        String spaces = "                                                             ";
        int curLen = str.length();
        if (curLen > length) return str;
        return str + spaces.substring(0, length - curLen);
    }

}














