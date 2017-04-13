package net.pisarenko.pcv.common;

import com.google.common.base.Joiner;

import java.time.LocalDateTime;
import java.util.Arrays;

import static java.lang.String.format;

public class Message {
    private final int[] data;
    private final MessageDirection direction;
    private final int seq;
    private final LocalDateTime timestamp;

    public Message(final int[] data, final MessageDirection direction, final int seq, final LocalDateTime timestamp) {
        this.data = Arrays.copyOf(data, 64);
        this.direction = direction;
        this.seq = seq;
        this.timestamp = timestamp;
    }

    public MessageDirection getDirection() {
        return direction;
    }

    public Command getCommand() {
        return Command.fromInt(data[5] << 8 | data[4]);
    }

    public int getLength() {
        return (data[7] << 8) | data[6];
    }

    public int[] getPayloadArray() {
        final int length = getLength();
        int[] payload = new int[length];
        System.arraycopy(data, 8, payload, 0, length);
        return payload;
    }

    public String[] getPayloadArrayHex() {
        final int[] payload = getPayloadArray();
        final String[] payloadHex = new String[payload.length];
        for (int i = 0; i < payload.length; i++) {
            payloadHex[i] = format("%02X", payload[i]);
        }
        return payloadHex;
    }

    public long getPayloadLong() {
        return join(getPayloadArray());
    }

    public long getPayloadFragment(final int start, final int length) {
        final int[] payload = getPayloadArray();
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
                seq, direction == MessageDirection.DOWN ? "D" : "U", Joiner.on(" ").join(getPayloadArrayHex()), data.length, timestamp);
    }

    public enum MessageDirection {
        /** DOWN means from host (PC) to device. */
        DOWN,
        /** UP means from device to host (PC). */
        UP
    }

    private static long join(final int[] array) {
        long out = 0;

        for (int i = 0; i < array.length; i++) {
            out |= array[i] << (i * 8);
        }

        return out;
    }
}
