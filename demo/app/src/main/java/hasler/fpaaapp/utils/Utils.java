package hasler.fpaaapp.utils;

import com.ftdi.j2xx.D2xxManager;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.*;

public class Utils {

    public static void debugLine(String s, boolean TF) {
        FileWriter write;
        PrintWriter print;
        //File file = new File("/storage/emulated/0/Download/debug.txt");

        try {
            write = new FileWriter("/storage/emulated/0/Download/debug.txt", TF);
            print = new PrintWriter(write);
        } catch (IOException e) {
            return;
        }
        print.printf("%s" + "%n", s);
        print.close();
    }
    /**
     * Where to write to
     * @param p The PrintWriter (e.g. System.out)
     * @param d The data to write
     */
    public static void printReadData(PrintStream p, byte... d) {
        p.println(join(swapBytes(reverse(d))));
    }

    /**
     * Print out an array
     * @param d The data to print out
     */
    public static void print(PrintStream p, byte... d) {
        p.println(join(d));
    }

    /**
     * Serialize character data as a string of hex values
     * @param c Character data to serialize
     * @return The string of hex values
     */
    public static String join(char... c) {
        StringBuilder sb = new StringBuilder();
        for (char aC : c) {
            sb.append(String.format("%04x ", (int) aC));
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Serialize some bytes as a string of hex values
     * @param b Byte data to serialize
     * @return The string of hex values
     */
    public static String join(byte... b) {
        StringBuilder s = new StringBuilder();
        for (byte aB : b) {
            s.append(String.format("%02x ", (byte) aB));
        }
        if (s.length() > 0) {
            return s.substring(0,  s.length() - 1);
        }
        return s.toString();
    }

    /**
     * Converts some bytes to an integer (assumes least significant value last)
     * @param data Data to convert
     * @return The resulting integer
     */
    public static int toInt(byte... data) {
        int i = 0;
        for (byte b : data) {
            i = i << 8;
            i += b & 0xff;
        }
        return i;
    }

    public static long[] toInts(byte... b) {
        return toInts(1, b);
    }

    /**
     * Convert a byte array to ints
     * @param b The array to convert
     * @return The resulting array of integers
     */
    public static long[] toInts(int l, byte... b) {
        long[] t = new long[b.length / l];
        for (int i = 0; i < t.length; i++) {
            for (int j = 0; j < l; j++) {
                t[i] = (t[i] << 8) | (b[l*i+j] & 0xff);
            }
        }
        return t;
    }

    /**
     * Converts an int array to a double array
     * @param t The array to convert
     * @return The resulting array of doubles
     */
    public static double[] toDoubles(long... t) {
        double[] d = new double[t.length];
        for (int i = 0; i < t.length; i++) d[i] = (double) t[i];
        return d;
    }

    /**
     * Converts a byte array to a double array
     * @param b The array to convert
     * @return The resulting array of doubles
     */
    public static double[] toDoubles(byte... b) {
        return toDoubles(toInts(2, b));
    }

    public static List<long[]> ffsplit(long[] d) {
        int n = 0;
        List<long[]> l = new ArrayList<>();
        for (int i = 0; i < d.length; i++) {
            if (d[i] == 0xFFFF || d[i] == 0x3333) {
                l.add(Arrays.copyOfRange(d, n, i));
                n = i+1;
            }
        }
        l.add(Arrays.copyOfRange(d, n, d.length));
        return l;
    }

    /**
     * Swaps consecutive bytes in an array. This is done in-place, but the data array is returned for syntactic sugar such as Utils.join(Utils.swapBytes(data))
     * Example: { 0x01, 0x02, 0x03, 0x04, 0x05 } -> { 0x02, 0x01, 0x04, 0x03, 0x05 }
     * @param data The data to swap
     * @return The data array, with bytes swapped
     */
    public static byte[] swapBytes(byte... data) {
        for (int i = 0; i < data.length / 2; i++) {
            byte temp = data[2*i];
            data[2*i] = data[2*i+1];
            data[2*i+1] = temp;
        }
        return data;
    }

    /**
     * Reverse an array of bytes. This is done in-place, but the data array is returned for syntactic sugar such as Utils.swapBytes(Utils.join(data))
     * Example: { 0x01, 0x02, 0x03 } -> { 0x03, 0x02, 0x01 }
     * @param data The data to reverse
     * @return The data array, with bytes reversed
     */
    public static byte[] reverse(byte... data) {
        for (int i = 0; i < data.length / 2; i++) {
            byte temp = data[data.length - i - 1];
            data[data.length - i - 1] = data[i];
            data[i] = temp;
        }
        return data;
    }

    public static long[] reverse(long... data) {
        for (int i = 0; i < data.length / 2; i++) {
            long temp = data[data.length - i - 1];
            data[data.length - i - 1] = data[i];
            data[i] = temp;
        }
        return data;
    }

    /**
     * Converts an array of chars to bytes, where the first byte is the least significant byte of the first char
     * Example: { 0x1234, 0x5678 } -> { 0x34, 0x12, 0x78, 0x56 }
     * @param data The data to convert
     * @return The converted data
     */
    public static byte[] toByte(char... data) {
        byte[] b = new byte[data.length * 2];
        int i = 0;
        for (int c : data) {
            b[i++] = (byte) (c);
            b[i++] = (byte) (c >> 8);
        }
        return b;
    }

    /**
     * Converts an array of ints to bytes, where the first byte is the least significant byte of the first int
     * Example: { 0x1234, 0x5678 } -> { 0x34, 0x12, 0x78, 0x56 }
     * @param data The data to convert
     * @return The converted data
     */
    public static byte[] toByte(int... data) {
        int n = data.length;
        for (int c : data) {
            int d = c >> 8;
            while (d != 0) {
                d >>= 8;
                n++;
            }
        }
        byte[] b = new byte[n];
        int i = 0;
        for (int c : data) {
            b[i++] = (byte) (c);
            int d = c >> 8;
            while (d != 0) {
                b[i++] = (byte) d;
                d >>= 8;
            }
        }
        return b;
    }

    /**
     * Unzips data in a zip file into byte arrays mapped by their file names
     * @param path Path to the zip file
     * @return The read data from the zip file
     */
    public static Map<String,byte[]> getZipContents(String path) {
        Map<String,byte[]> contents = new HashMap<>();

        InputStream is;
        ZipInputStream zis;
        try {
            is = new FileInputStream(path);
            zis = new ZipInputStream(new BufferedInputStream(is));
            byte[] buffer = new byte[1024];
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                // Cannot handle directories
                if (ze.isDirectory()) {
                    continue;
                }

                String name = ze.getName();

                byte[] data = new byte[(int) ze.getSize()];

                int count, total = 0;
                while ((count = zis.read(buffer)) != -1) {
                    int i = 0;
                    while (i < count) {
                        data[total++] = buffer[i++];
                    }
                }

                zis.closeEntry();

                contents.put(name, data);
            }

            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contents;
    }

    /**
     * Basically Integer.parseInt(n, 16). Bytes are ASCII-encoded hex values (e.g. "0x5000"). This was necessary for doing a particular operation.
     * @param b The bytes to parse
     * @return The parsed bytes
     */
    public static byte[] parseHexAscii(byte[] b) {
        byte[] r = new byte[(b.length - 1) * 2 / 7];
        int j = 0;
        for (int i = 0; i < b.length - 7; i += 7) {
            r[j++] = (byte) ((parseHexAscii(b[i+2]) << 4) + parseHexAscii(b[i+3]));
            r[j++] = (byte) ((parseHexAscii(b[i+4]) << 4) + parseHexAscii(b[i+5]));
        }
        return r;
    }

    /**
     * Helper method for the above method. Could make public? Might be unnecessarily cluttered
     * @param b The byte to parse, in ASCII. e.g. 'a' -> 97, '0' -> 48
     * @return The parsed byte, e.g. 'a' -> 0xa, '0' -> 0x0
     */
    private static byte parseHexAscii(byte b) {
        return (b >= 'a') ? (byte) (b - 'a' + 10) : (byte) (b - '0');
    }

    /**
     * Convert ELF binary data to the bytes to program to the MSP430
     * @param data The data to convert
     * @return The converted data
     * @throws IOException If the data was invalid
     */
    public static byte[] compileElf(byte[] data) throws IOException {
        // Make a new buffer to read data
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // Read part of the header to get information about the file
        byte[] ident = new byte[16];
        buffer.get(ident);

        // Check signing to make sure it is an ELF file
        if (ident[0] != 0x7f || ident[1] != 'E' || ident[2] != 'L' || ident[3] != 'F') {
            throw new IOException("Signing is invalid: This is not an ELF file");
        }

        // Get format (32 bit or 64 bit)
        int format;
        switch (ident[4]) {
            case 1: format = 32; break;
            case 2: format = 64; break;
            default: throw new IOException("Invalid ELF class");
        }

        // Get endianess
        switch (ident[5]) {
            case 1: buffer.order(ByteOrder.LITTLE_ENDIAN); break;
            case 2: buffer.order(ByteOrder.BIG_ENDIAN); break;
            default: throw new IOException("Invalid ELF endian");
        }

        // For reading sections
        int start = (format == 32) ? buffer.getInt(32) : buffer.getInt(40);
        int entry_size = (format == 32) ? buffer.getShort(46) & 0xffff : buffer.getShort(58) & 0xffff;
        int n_entries = (format == 32) ? buffer.getShort(48) & 0xffff : buffer.getShort(60) & 0xffff;

        // Extract relevant information about entries
        ElfEntry[] entries = new ElfEntry[n_entries];
        for (int i = 0; i < n_entries; i++) {
            int offset = start + i * entry_size;
            ElfEntry entry = new ElfEntry();
            entry.address = (format == 32) ? buffer.getLong(offset + 12) & 0xffffffffL : buffer.getLong(offset + 16);
            entry.offset = (format == 32) ? buffer.getLong(offset + 16) & 0xffffffffL : buffer.getLong(offset + 24);
            entry.size = (format == 32) ? buffer.getLong(offset + 20) & 0xffffffffL : buffer.getLong(offset + 32);
            entries[i] = entry;
        }

        // Get the two relevant entries
        ElfEntry tex = entries[1], vec = entries[2];

        byte[] output_data = new byte[(int) (vec.address + vec.size) / 4];
        for (int i = 0; i < tex.size; i++) {
            output_data[i] = data[i + (int) tex.offset];
        }
        for (int i = 0; i < vec.size; i++) {
            output_data[output_data.length - i - 1] = data[(int) (vec.offset + vec.size) - i - 1];
        }

        return output_data;
    }

    static class ElfEntry {
        long address, size, offset;
    }
    private static Scanner s;

    /**
     * Tool for converting commands to MSP430 bytes
     * @param in The input stream (for example, System.in)
     * @param out The output stream (for example, System.out)
     */
    public static void interpreter(InputStream in, PrintStream out) {
        s = new Scanner(in);

        String command, action;

        while (true) {
            out.print("Command: ");
            command = s.next();
            out.print("Action: ");
            action = s.next();
            out.println("Compiled: " + toHex(compile(command, action)));
        }
    }

    /**
     * Tool for converting MSP430 bytes to commands
     * @param in The input stream (for example, System.in)
     * @param out The output stream (for example, System.out)
     */
    public static void uninterpreter(InputStream in, PrintStream out) {
        s = new Scanner(in);

        String command;
        byte output;

        while (true) {
            command = s.next();
            output = (byte) Integer.parseInt(command, 16);
            out.println(uncompile(output));
        }
    }

    /**
     * toHex: Print the hex representation of a byte
     */
    protected static String toHex(byte n) {
        return String.format("0x%02x", n);
    }

    /**
     * Uncompiler: Given a binary command, figure out what it is in as assembly language
     */
    public static String uncompile(byte output) {
        String action;

        if ((output & 0x80) == 0) {
            action = "RD";
        } else {
            action = "WR";
        }

        switch(output & 0x7F) {
            case (0x00 | 0x00): return "CPU_ID_LO " + action;
            case (0x00 | 0x01): return "CPU_ID_HI " + action;
            case (0x40 | 0x02): return "CPU_CTL " + action;
            case (0x40 | 0x03): return "CPU_STAT " + action;
            case (0x40 | 0x04): return "MEM_CTL " + action;
            case (0x00 | 0x05): return "MEM_ADDR " + action;
            case (0x00 | 0x06): return "MEM_DATA " + action;
            case (0x00 | 0x07): return "MEM_CNT " + action;
            case (0x40 | 0x08): return "BRK0_CTL " + action;
            case (0x40 | 0x09): return "BRK0_STAT " + action;
            case (0x00 | 0x0A): return "BRK0_ADDR0 " + action;
            case (0x00 | 0x0B): return "BRK0_ADDR1 " + action;
            case (0x40 | 0x0C): return "BRK1_CTL " + action;
            case (0x40 | 0x0D): return "BRK1_STAT " + action;
            case (0x00 | 0x0E): return "BRK1_ADDR0 " + action;
            case (0x00 | 0x0F): return "BRK1_ADDR1 " + action;
            case (0x40 | 0x10): return "BRK2_CTL " + action;
            case (0x40 | 0x11): return "BRK2_STAT " + action;
            case (0x00 | 0x12): return "BRK2_ADDR0 " + action;
            case (0x00 | 0x13): return "BRK2_ADDR1 " + action;
            case (0x40 | 0x14): return "BRK3_CTL " + action;
            case (0x40 | 0x15): return "BRK3_STAT " + action;
            case (0x00 | 0x16): return "BRK3_ADDR0 " + action;
            case (0x00 | 0x17): return "BRK3_ADDR1 " + action;
            case (0x00 | 0x18): return "CPU_NR " + action;
            default:			return toHex(output);
        }
    }

    /**
     * Compiler: Turn a command and an action into a bit representation
     */
    public static byte compile(String command, String action) {
        byte rd_wr;

        switch (action.toUpperCase()) {
            case "RD":				rd_wr = (byte) 0x00; break;
            case "WR":				rd_wr = (byte) 0x80; break;
            default:				rd_wr = (byte) 0x00; break;
        }

        switch (command.toUpperCase()) {
            case "CPU_ID_LO":		return (byte) (rd_wr | 0x00 | 0x00);
            case "CPU_ID_HI":		return (byte) (rd_wr | 0x00 | 0x01);
            case "CPU_CTL":			return (byte) (rd_wr | 0x40 | 0x02);
            case "CPU_STAT":		return (byte) (rd_wr | 0x40 | 0x03);
            case "MEM_CTL":			return (byte) (rd_wr | 0x40 | 0x04);
            case "MEM_ADDR":		return (byte) (rd_wr | 0x00 | 0x05);
            case "MEM_DATA":		return (byte) (rd_wr | 0x00 | 0x06);
            case "MEM_CNT":			return (byte) (rd_wr | 0x00 | 0x07);
            case "BRK0_CTL":		return (byte) (rd_wr | 0x40 | 0x08);
            case "BRK0_STAT":		return (byte) (rd_wr | 0x40 | 0x09);
            case "BRK0_ADDR0":		return (byte) (rd_wr | 0x00 | 0x0A);
            case "BRK0_ADDR1":		return (byte) (rd_wr | 0x00 | 0x0B);
            case "BRK1_CTL":		return (byte) (rd_wr | 0x40 | 0x0C);
            case "BRK1_STAT":		return (byte) (rd_wr | 0x40 | 0x0D);
            case "BRK1_ADDR0":		return (byte) (rd_wr | 0x00 | 0x0E);
            case "BRK1_ADDR1":		return (byte) (rd_wr | 0x00 | 0x0F);
            case "BRK2_CTL":		return (byte) (rd_wr | 0x40 | 0x10);
            case "BRK2_STAT":		return (byte) (rd_wr | 0x40 | 0x11);
            case "BRK2_ADDR0":		return (byte) (rd_wr | 0x00 | 0x12);
            case "BRK2_ADDR1":		return (byte) (rd_wr | 0x00 | 0x13);
            case "BRK3_CTL":		return (byte) (rd_wr | 0x40 | 0x14);
            case "BRK3_STAT":		return (byte) (rd_wr | 0x40 | 0x15);
            case "BRK3_ADDR0":		return (byte) (rd_wr | 0x00 | 0x16);
            case "BRK3_ADDR1":		return (byte) (rd_wr | 0x00 | 0x17);
            case "CPU_NR":			return (byte) (rd_wr | 0x00 | 0x18);
            default:				return (0x00);
        }
    }

    public static final String[] ADDRESSES = {
            "CPU_ID_LO",	"CPU_ID_HI",	"CPU_CTL",
            "CPU_STAT",		"MEM_CTL",		"MEM_ADDR",
            "MEM_DATA",		"MEM_CNT",		"BRK0_CTL",
            "BRK0_STAT",	"BRK0_ADDR0",	"BRK0_ADDR1",
            "BRK1_CTL",		"BRK1_STAT",	"BRK1_ADDR0",
            "BRK1_ADDR1",	"BRK2_CTL",		"BRK2_STAT",
            "BRK2_ADDR0",	"BRK2_ADDR1",	"BRK3_CTL",
            "BRK3_STAT",	"BRK3_ADDR0",	"BRK3_ADDR1",
            "CPU_NR",
    };

	/* ABOUT
	 * First bit (i.e. 0x80 or 0b10000000): Set to write, otherwise read
	 * Second bit (i.e. 0x40 or 0b01000000): Set for 8 bit, otherwise 16 bit
	 * Rest: Address to particular register
	 */

    public static class D2xxUtils {
        public static String getType(int type) {
            switch (type) {
                case D2xxManager.FT_DEVICE_232B:        return "FT232B device";
                case D2xxManager.FT_DEVICE_8U232AM:     return "FT8U232AM device";
                case D2xxManager.FT_DEVICE_UNKNOWN:     return "Unknown device";
                case D2xxManager.FT_DEVICE_2232:        return "FT2232 device";
                case D2xxManager.FT_DEVICE_232R:        return "FT232R device";
                case D2xxManager.FT_DEVICE_2232H:       return "FT2232H device";
                case D2xxManager.FT_DEVICE_4232H:       return "FT4232H device";
                case D2xxManager.FT_DEVICE_232H:        return "FT232H device";
                case D2xxManager.FT_DEVICE_X_SERIES:    return "FTDI X_SERIES";
                case D2xxManager.FT_DEVICE_4222_0:
                case D2xxManager.FT_DEVICE_4222_1_2:
                case D2xxManager.FT_DEVICE_4222_3:      return "FT4222 device";
                default:                                return "FT232B device";

            }
        }
    }
}
