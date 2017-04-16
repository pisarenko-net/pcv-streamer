package net.pisarenko.pcv.common;

import com.google.common.collect.Lists;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Parse HHD Device Monitoring Studio text file output to reconstruct PCV USB messages.
 */
public class PacketUtil {
    private static final Pattern DATA_LINE_PATTERN = Pattern.compile("^.*(([0-9a-f][0-9a-f]\\s){16}+).*$");
    private static final Pattern DATA_DIRECTION_DOWN_PATTERN = Pattern.compile("^.*Direction.*\"Down\".*$");
    private static final Pattern DATA_DIRECTION_UP_PATTERN = Pattern.compile("^.*Direction.*\"Up\".*$");
    private static final Pattern SEQ_PATTERN = Pattern.compile("^(\\d+)\\s+.*$");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^.*(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d\\s\\d\\d:\\d\\d:\\d\\d)\\s+$");

    private PacketUtil() {}

    public static String[] bytesToHexStrings(final byte[] array) {
        final String[] payloadHex = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            payloadHex[i] = format("%02X", array[i]);
        }
        return payloadHex;
    }

    public static List<Packet> parseUSBPackets(final String path) throws Exception {
        final List<Packet> packets = Lists.newArrayList();

        int dataLineCount = 0;
        byte[] messageData = new byte[64];
        Packet.PacketDirection direction = Packet.PacketDirection.DOWN;
        int seq = 0;
        String timestamp = null;

        for (String line : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_16LE)) {
            Matcher dataLineMatcher = DATA_LINE_PATTERN.matcher(line);
            Matcher upDirectionMatcher = DATA_DIRECTION_UP_PATTERN.matcher(line);
            Matcher downDirectionMatcher = DATA_DIRECTION_DOWN_PATTERN.matcher(line);
            Matcher seqMatcher = SEQ_PATTERN.matcher(line);
            Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(line);

            if (upDirectionMatcher.matches()) {
                direction = Packet.PacketDirection.UP;
            } else if (downDirectionMatcher.matches()) {
                direction = Packet.PacketDirection.DOWN;
            }

            if (seqMatcher.matches()) {
                seq = Integer.parseInt(seqMatcher.group(1));
            }

            if (timestampMatcher.matches()) {
                timestamp = timestampMatcher.group(1);
            }

            if (dataLineMatcher.matches()) {
                byte[] data = intsToBytes(Arrays.stream(dataLineMatcher.group(1).split("\\s"))
                        .mapToInt(s -> Integer.parseInt(s, 16)).toArray());
                System.arraycopy(data, 0, messageData, dataLineCount * 16, 16);
                dataLineCount++;
                if (dataLineCount == 4) {
                    final Packet packet = Packet.createFromDumpData(messageData, direction, seq, parseTimestamp(timestamp));
                    packets.add(packet);
                    dataLineCount = 0;
                    messageData = new byte[64];
                }
            }
        }

        return packets;
    }

    private static byte[] intsToBytes(final int[] ints) {
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte)ints[i];
        }
        return bytes;
    }

    private static LocalDateTime parseTimestamp(final String timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return LocalDateTime.parse(timestamp, formatter);
    }
}