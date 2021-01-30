package net.scheffers.robot.simplexfs;

import jutils.JUtils;
import jutils.database.BytePool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class SimplexFSCommandLine {

    public static void main(String[] args) {
        JUtils.getArgs(args);
        if (!JUtils.getArg("mksx").equals("null") && JUtils.getArg("unsx").equals("null")) {
            try {
                if (JUtils.getArg("size").equals("null")) {
                    System.err.println("Error: no size declared.");
                    return;
                }
                String sizeStr = JUtils.getArg("size").toLowerCase();
                int blocks = 0;
                try {
                    if (sizeStr.matches("[0-9]+kb")) {
                        blocks = Integer.parseInt(sizeStr.substring(0, sizeStr.length() - 2)) * 4;
                    }
                    else
                    {
                        blocks = Integer.parseInt(sizeStr);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Number format was not right, did you mean:\n  -size 123KB\n  -size 123");
                    return;
                }
                makeSimplexFS(JUtils.getArg("in"), JUtils.getArg("out"), blocks);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (!JUtils.getArg("unsx").equals("null") && JUtils.getArg("enbin").equals("null") && JUtils.getArg("unbin").equals("null")) {
            try {
                readSimplexFS(JUtils.getArg("in"), JUtils.getArg("out"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("Options:\n" +
                    "  -mksx     Make a new simplex filesystem (-in is optional).\n" +
                    "  -unsx     Read a simplex filesystem into a directory (works for both disk image and logisim's proprietary bullshit file).\n" +
                    "  -size [s] Size of the filesystem to be made.\n" +
                    "            123 - 123 blocks of 256 bytes each. Max: 65535\n" +
                    "            64KB - 64 kilobytes (65 536 bytes) of capacity in total. Max: 16383KB\n" +
                    "  -vn [n]   Specify volume name for filesystem to be made.\n" +
                    "  -in [f]   Input file.\n" +
                    "  -out [f]  Output file.\n" +
                    "  -lhf      Use logisim's proprietary bullshit file for image file format."
            );
        }
    }

    public static void makeSimplexFS(String src, String dest, int numBlocks) throws Exception {
        String voln = null;
        if (!JUtils.getArg("vn").equals("null")) {
            voln = JUtils.getArg("vn");
        }
        File in = null;
        if (!src.equals("null")) {
            in = new File(JUtils.getArg("in"));
        }
        if (JUtils.getArg("lhf").equals("true")) {
            SimplexFS.makeSimplexFS(new LogisimBullshitOutputStream(new File(dest)), voln, numBlocks, (byte) 0x00, new Random().nextInt(), in);
        }
        else
        {
            SimplexFS.makeSimplexFS(new FileOutputStream(new File(dest)), voln, numBlocks, (byte) 0x00, new Random().nextInt(), in);
        }
    }

    public static void readSimplexFS(String src, String dest) throws Exception {
        if (JUtils.getArg("lhf").equals("true")) {
            SimplexFS.unpackSimplexFS(new LogisimBullshitInputStream(new File(src)), new File(dest), null);
        }
        else
        {
            SimplexFS.unpackSimplexFS(new FileInputStream(new File(src)), new File(dest), null);
        }
    }

    public static void enbin(String src, String dest) throws IOException {
        FileInputStream in = new FileInputStream(new File(src));
        FileOutputStream out = new FileOutputStream(new File(dest));
        out.write("v2.0 raw\n".getBytes(StandardCharsets.US_ASCII));
        char[] hexes = "0123456789ABCDEF".toCharArray();
        int len = 0;
        BytePool pool = new BytePool();
        while (in.available() > 0) {
            byte[] buf = new byte[in.available()];
            in.read(buf, 0, buf.length);
            pool.addBytes(buf);
        }
        byte[] buf = pool.copyToArray();
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] != 0) {
                len = i;
            }
        }
        for (int i = 0; i < len - 1; i++) {
            out.write(hexes[(buf[i] >> 4) & 0xf]);
            out.write(hexes[buf[i] & 0xf]);
            out.write(' ');
        }
        out.write(hexes[(buf[len - 1] >> 4) & 0xf]);
        out.write(hexes[buf[len - 1] & 0xf]);
        out.flush();
        out.close();
        in.close();
    }

    public static void unbin(String src, String dest) throws IOException {
        FileInputStream in = new FileInputStream(new File(src));
        FileOutputStream out = new FileOutputStream(new File(dest));
        String hexes = "0123456789ABCDEF";
        while (in.available() > 0) {
            int read = in.read();
            if (read == '\n') {
                break;
            }
        }
        int stuff = 0;
        while (in.available() > 0) {
            int read = in.read();
            if (read == ' ') {
                out.write(stuff);
                stuff = 0;
            }
            else
            {
                stuff <<= 4;
                stuff |= hexes.indexOf((char) read);
            }
        }
        out.flush();
        out.close();
        in.close();
    }

}
