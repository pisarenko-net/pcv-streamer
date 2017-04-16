package net.pisarenko.pcv.common;

import com.google.common.base.Joiner;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;

import static java.lang.String.format;

public class Packet {
    /** Complete 64-byte packet. */
    private byte[] data;
    /** See {@link PacketDirection} */
    private PacketDirection direction;
    /** Sequence number. Useful when working with existing dumps. */
    private int seq;
    /** When was the packet received/captured. */
    private LocalDateTime timestamp;

    private static final Random random = new Random();
    private static final Clock clock = Clock.systemUTC();

    private Packet() {}

    public static Packet createFromDumpData(final byte[] data, final PacketDirection direction, final int seq, final LocalDateTime timestamp) {
        Packet packet = new Packet();
        packet.data = Arrays.copyOf(data, 64);
        packet.direction = direction;
        packet.seq = seq;
        packet.timestamp = timestamp;
        return packet;
    }

    public static Packet createFromReceivedData(final byte[] data, final LocalDateTime timestamp) {
        Packet packet = new Packet();
        packet.data = Arrays.copyOf(data, 64);
        packet.direction = PacketDirection.UP;
        packet.seq = 0;
        packet.timestamp = timestamp;
        return packet;
    }

    public static Packet createSendPacket(final Command command, final byte[] payload) {
        Packet packet = new Packet();
        packet.data = new byte[64];
        packet.seq = 0;
        packet.timestamp = LocalDateTime.now(clock);
        packet.direction = PacketDirection.DOWN;

        // set packet ID
        final int randInt = random.nextInt();
        packet.data[0] = (byte)(randInt & 0xFF);
        packet.data[1] = (byte)((randInt >> 8) & 0xFF);
        packet.data[2] = (byte)((randInt >> 16) & 0xFF);
        packet.data[3] = (byte)((randInt >> 24) & 0xFF);

        // set command type
        final int cmdValue = command.toValue();
        packet.data[4] = (byte)(cmdValue & 0xFF);
        packet.data[5] = (byte)((cmdValue >> 8) & 0xFF);

        // set payload length
        packet.data[6] = (byte)(payload.length & 0xFF);
        packet.data[7] = (byte)((payload.length >> 8) & 0xFF);

        // set payload
        for (int i = 0; i < payload.length; i++) {
            packet.data[8 + i] = payload[i];
        }

        return packet;
    }

    public long getId() {
        return joinBytes(data, 0, 4);
    }

    public byte[] getRawPacket() {
        byte[] out = new byte[64];
        System.arraycopy(this.data, 0, out, 0, 64);
        return out;
    }

    public PacketDirection getDirection() {
        return direction;
    }

    public Command getCommand() {
        return Command.fromInt(data[5] << 8 | data[4]);
    }

    public int getPayloadLength() {
        return (data[7] << 8) | data[6];
    }

    public byte[] getRawPayload() {
        final int length = getPayloadLength();
        byte[] payload = new byte[length];
        System.arraycopy(data, 8, payload, 0, length);
        return payload;
    }

    public long getPayloadAsLong() {
        return joinBytes(getRawPayload(), 0, getPayloadLength());
    }

    public long getPayloadFragment(final int start, final int length) {
        return joinBytes(getRawPayload(), start, length);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return format("[%06d] [%s] [%s] [%s] @ %s",
                seq, direction == PacketDirection.DOWN ? "D" : "U", Joiner.on(" ").join(PacketUtil.bytesToHexStrings(getRawPacket())), data.length, timestamp);
    }

    public enum PacketDirection {
        /** DOWN means from host (PC) to device. */
        DOWN,
        /** UP means from device to host (PC). */
        UP
    }

    private static long joinBytes(final byte[] array, final int start, final int length) {
        long out = 0;
        int c = 0;

        for (int i = start; i < start + length; i++, c++) {
            out |= (array[i] & 0xFF) << (c * 8);
        }

        return out;
    }
}
