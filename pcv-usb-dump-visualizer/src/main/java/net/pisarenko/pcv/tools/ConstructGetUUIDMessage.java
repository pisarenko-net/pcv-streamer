package net.pisarenko.pcv.tools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Code snippets for alleged PCV message construction.
 */
public class ConstructGetUUIDMessage {
    public static final int MAX_PACKET_SIZE = 40;

    public static void main(String[] args) {
        final ByteBuffer message = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
        message.putInt(0, 131072);
        message.putShort(4, (short)16);

        Random random = new Random();
        int route = random.nextInt();

        byte[] data = new byte[40];

        final ByteBuffer report1 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        report1.putInt(route);
        System.arraycopy(report1.array(), 0, data, 0, 4);
        final ByteBuffer report2 = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        report2.putShort((short)6);
        System.arraycopy(report2.array(), 0, data, 4, 2);
        final ByteBuffer report3 = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        report3.putShort((short)(message.array().length + 4));
        System.arraycopy(report3.array(), 0, data, 6, 2);

        int i = Math.min(message.array().length, MAX_PACKET_SIZE - 8);
        System.arraycopy(message.array(), 0, data, 8, i);

        byte[] o = new byte[data.length];
        System.arraycopy(data, 0, o, 0, data.length);

        System.out.println(bytesToHex(report3.array()));
        System.out.println(bytesToHex(message.array()));
        System.out.println(bytesToHex(data));
        System.out.println(bytesToHex(o));
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
