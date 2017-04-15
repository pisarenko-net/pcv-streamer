package net.pisarenko.pcv.common;

import com.google.common.base.Joiner;

import java.time.LocalDateTime;
import java.util.Arrays;

import static java.lang.String.format;

public class Packet {
    private int id;
    private final byte[] data;
    private final PacketDirection direction;
    private final int seq;
    private final LocalDateTime timestamp;

    public Packet(final byte[] data, final PacketDirection direction, final int seq, final LocalDateTime timestamp) {
        this.data = Arrays.copyOf(data, 64);
        this.id = (int) getPayloadFragment(0, 4);
        this.direction = direction;
        this.seq = seq;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
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
        return joinBytes(getRawPayload());
    }

    public long getPayloadFragment(final int start, final int length) {
        final byte[] payload = getRawPayload();
        long out = 0;
        int c = 0;
        for (int i = start; i < start + length; i++) {
            out |= payload[i] << (c * 8);
            c++;
        }
        return out;
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

    private static long joinBytes(final byte[] array) {
        long out = 0;

        for (int i = 0; i < array.length; i++) {
            out |= array[i] << (i * 8);
        }

        return out;
    }
}
