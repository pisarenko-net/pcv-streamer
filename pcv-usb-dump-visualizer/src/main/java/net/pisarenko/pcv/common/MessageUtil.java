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

/**
 * Parse HHD Device Monitoring Studio text file output to reconstruct PCV USB messages.
 */
public class MessageUtil {
    private static final Pattern DATA_LINE_PATTERN = Pattern.compile("^.*(([0-9a-f][0-9a-f]\\s){16}+).*$");
    private static final Pattern DATA_DIRECTION_DOWN_PATTERN = Pattern.compile("^.*Direction.*\"Down\".*$");
    private static final Pattern DATA_DIRECTION_UP_PATTERN = Pattern.compile("^.*Direction.*\"Up\".*$");
    private static final Pattern SEQ_PATTERN = Pattern.compile("^(\\d+)\\s+.*$");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^.*(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d\\s\\d\\d:\\d\\d:\\d\\d)\\s+$");

    private MessageUtil() {}

    public static List<Message> parseUsbMessage(final String path) throws Exception {
        final List<Message> messages = Lists.newArrayList();

        int dataLineCount = 0;
        int[] messageData = new int[64];
        Message.MessageDirection direction = Message.MessageDirection.DOWN;
        int seq = 0;
        String timestamp = null;

        for (String line : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_16LE)) {
            Matcher dataLineMatcher = DATA_LINE_PATTERN.matcher(line);
            Matcher upDirectionMatcher = DATA_DIRECTION_UP_PATTERN.matcher(line);
            Matcher downDirectionMatcher = DATA_DIRECTION_DOWN_PATTERN.matcher(line);
            Matcher seqMatcher = SEQ_PATTERN.matcher(line);
            Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(line);

            if (upDirectionMatcher.matches()) {
                direction = Message.MessageDirection.UP;
            } else if (downDirectionMatcher.matches()) {
                direction = Message.MessageDirection.DOWN;
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
                    final Message message = new Message(messageData, direction, seq, parseTimestamp(timestamp));
                    messages.add(message);
                    dataLineCount = 0;
                    messageData = new int[64];
                }
            }
        }

        return messages;
    }

    private static LocalDateTime parseTimestamp(final String timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return LocalDateTime.parse(timestamp, formatter);
    }
}