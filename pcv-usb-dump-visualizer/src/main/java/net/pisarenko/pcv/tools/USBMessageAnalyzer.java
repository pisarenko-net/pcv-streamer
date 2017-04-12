package net.pisarenko.pcv.tools;

import com.google.common.collect.Lists;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * This program parses text output captured from a PCV using the HHD Device Monitoring Studio Filter.
 *
 * The goal is to understand where are the useful data bits stored. The program is based on previous discoveries and
 * many assumptions.
 */
public class USBMessageAnalyzer {
    private static final boolean DIRECTION_DOWN = true;
    private static final boolean DIRECTION_UP = false;

    private static final Pattern DATA_LINE_PATTERN = Pattern.compile("^.*(([0-9a-f][0-9a-f]\\s){16}+).*$");
    private static final Pattern DATA_DIRECTION_DOWN_PATTERN = Pattern.compile("^.*Direction.*\"Down\".*$");
    private static final Pattern DATA_DIRECTION_UP_PATTERN = Pattern.compile("^.*Direction.*\"Up\".*$");
    private static final Pattern SEQ_PATTERN = Pattern.compile("^(\\d+)\\s+.*$");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^.*(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d\\s\\d\\d:\\d\\d:\\d\\d)\\s+$");

    public static void main(String[] args) throws Exception {
        final String usbDumpPath = args[0];
        final List<Message> messages = parseUsbMessage(usbDumpPath);

        messages.stream().filter(message -> message.direction==DIRECTION_UP).forEach(System.out::println);
    }

    private static List<Message> parseUsbMessage(final String path) throws Exception {
        final List<Message> messages = Lists.newArrayList();

        int dataLineCount = 0;
        int[] messageData = new int[64];
        boolean direction = false;
        int seq = 0;
        String timestamp = null;

        for (String line : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_16LE)) {
            Matcher dataLineMatcher = DATA_LINE_PATTERN.matcher(line);
            Matcher upDirectionMatcher = DATA_DIRECTION_UP_PATTERN.matcher(line);
            Matcher downDirectionMatcher = DATA_DIRECTION_DOWN_PATTERN.matcher(line);
            Matcher seqMatcher = SEQ_PATTERN.matcher(line);
            Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(line);

            if (upDirectionMatcher.matches()) {
                direction = DIRECTION_UP;
            } else if (downDirectionMatcher.matches()) {
                direction = DIRECTION_DOWN;
            }

            if (seqMatcher.matches()) {
                seq = Integer.parseInt(seqMatcher.group(1));
            }

            if (timestampMatcher.matches()) {
                timestamp = timestampMatcher.group(1);
            }

            if (dataLineMatcher.matches()) {
                int[] data = Arrays.stream(dataLineMatcher.group(1).split("\\s"))
                        .mapToInt(s -> Integer.parseInt(s, 16)).toArray();
                System.arraycopy(data, 0, messageData, dataLineCount * 16, 16);
                dataLineCount++;
                if (dataLineCount == 4) {
                    final Message message = new Message(messageData, direction, seq, timestamp);
                    messages.add(message);
                    dataLineCount = 0;
                    messageData = new int[64];
                }
            }
        }

        return messages;
    }

    private static String getDirectionString(final boolean direction) {
        return direction == DIRECTION_DOWN ? "DOWN" : "UP";
    }

    private static class Message {
        public final int[] data;
        private final boolean direction;
        private final String directionString;
        private final int seq;
        private final String timestamp;

        Message(final int[] data, final boolean direction, final int seq, final String timestamp) {
            this.data = Arrays.copyOf(data, 64);
            this.direction = direction;
            this.directionString = getDirectionString(direction);
            this.seq = seq;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return format("[%s] %s message %s (length %s) @ %s",
                    seq, directionString, Arrays.toString(data), data.length, timestamp);
        }
    }
}