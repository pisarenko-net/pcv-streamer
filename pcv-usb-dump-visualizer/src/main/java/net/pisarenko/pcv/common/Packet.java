package net.pisarenko.pcv.common;

import com.google.common.base.Joiner;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;

import static java.lang.String.format;

/**
 * A 64 byte PCV packet consists of:
 *  - 4 bytes ID (a random number)
 *  - 2 bytes command (see {@link Command})
 *  - 2 bytes payload length
 *  - remaining bytes payload and junk (to fill 64 byte packet)
 *
 *  Note: PCV packets are generally little endian! For example, assuming 0x010F stores a decimal value we need to reverse
 *  the bytes before converting to decimal. So 0x010F becomes 0x0F01 3841.
 */
public class Packet {
    /** Payload that requests engine statistics, e.g. RPM, throttle, speed, gear... */
    private static final byte[] REQUEST_PACKET_PAYLOAD = new byte[]{
            (byte) 0x1b, (byte) 0x1c, (byte) 0x2a, (byte) 0x2e, (byte) 0xc5,
            (byte) 0x8f, (byte) 0xc3, (byte) 0x1d, (byte) 0x1f, (byte) 0x8e,
            (byte) 0xe0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

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

    public static Packet createStatsRequestPacket() {
        return createSendPacket(Command.GET_CHANNEL_STATUS, REQUEST_PACKET_PAYLOAD);
    }

    /**
     * When PCV responds to a sent packet it will reuse the ID. So an ID is a way to correlate responses to previously
     * sent requests.
     */
    public long getId() {
        return joinBytes(data, 0, 4);
    }

    /**
     * Returns a complete copy of the raw packet byte array. Note: little endian!
     */
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

    /**
     * Returns a complete copy of the raw payload byte array. Note: little endian!
     */
    public byte[] getRawPayload() {
        final int length = getPayloadLength();
        byte[] payload = new byte[length];
        System.arraycopy(data, 8, payload, 0, length);
        return payload;
    }

    /**
     * Returns a long by joining together all payload bytes. Note: may overflow!
     */
    public long getPayloadAsLong() {
        return joinBytes(getRawPayload(), 0, getPayloadLength());
    }

    /**
     * Returns a long by joining together specified bytes in the payload. Note: may overflow!
     */
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
