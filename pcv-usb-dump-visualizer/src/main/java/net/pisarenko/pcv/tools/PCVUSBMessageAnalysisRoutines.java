package net.pisarenko.pcv.tools;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import net.pisarenko.pcv.common.Command;
import net.pisarenko.pcv.common.Message;
import net.pisarenko.pcv.common.MessageUtil;
import net.pisarenko.pcv.values.Throttle;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This program parses text output captured from a PCV using the HHD Device Monitoring Studio Filter.
 *
 * The goal is to understand where are the useful data bits stored. The program is based on previous discoveries and
 * many assumptions.
 */
public class PCVUSBMessageAnalysisRoutines {
    public static void main(String[] args) throws Exception {
        final String usbDumpPath = args[0];
        final List<Message> messages = MessageUtil.parseUsbMessage(usbDumpPath);
        final String routine = args[1];

        switch (routine) {
            case "all":
                messages.forEach(System.out::println);
                break;
            case "up":
                printChannelStatusMessages(messages, Message.MessageDirection.UP);
                break;
            case "down":
                printChannelStatusMessages(messages, Message.MessageDirection.DOWN);
                break;
            case "1":
                analyzeDifferingBitsInUpChannelStatusMessages(messages);
                break;
            case "2":
                System.out.println("\n\nAll 4 unique values observed with throttle @86 and @19");
                printDifferingBits(new Long[]{319258369l, 319257857l, 319155457l, 319155969l});
                System.out.println("\n\nOften occurring values observed throttle @86 and @19");
                printDifferingBits(new Long[]{319258369l, 319155457l});
                System.out.println("\n\nSeldom occurring values observed throttle @86 and @19");
                printDifferingBits(new Long[]{319257857l, 319155969l});
                System.out.println("\n\nSeldom throttle @86 often @19");
                printDifferingBits(new Long[]{319257857l, 319155457l});
                System.out.println("\n\nOften throttle @86 seldom @19");
                printDifferingBits(new Long[]{319258369l, 319155969l});
                break;
            case "3":
                analyzeAllegedThrottleValues(messages);
                break;
            default:
                System.out.println("Unknown routine");
        }
    }

    private static void printChannelStatusMessages(final List<Message> messages, Message.MessageDirection direction) {
        messages.stream()
                .filter(m -> m.getDirection() == direction)
                .filter(m -> m.getCommand() == Command.GET_CHANNEL_STATUS)
                .forEach(System.out::println);
    }

    private static void analyzeAllegedThrottleValues(final List<Message> messages) {
        messages.stream()
                .filter(m -> m.getDirection() == Message.MessageDirection.UP)
                .filter(m -> m.getCommand() == Command.GET_CHANNEL_STATUS)
                .filter(m -> m.getLength() == 29)
                .forEach(m -> System.out.println("[" + Joiner.on(" ").join(m.getPayloadArrayHex()) + "] " + Throttle.fromMessage(m)));
    }

    /**
     * Take unique values of the "get channel status" command and see in what bits do they differ.
     */
    private static void analyzeDifferingBitsInUpChannelStatusMessages(final List<Message> messages) {
        List<Long> payloads = messages.stream()
                .filter(message -> message.getDirection()== Message.MessageDirection.UP)
                .filter(message -> message.getCommand() == Command.GET_CHANNEL_STATUS)
                .map(Message::getPayloadLong).collect(Collectors.toList());

        TreeSet<Long> payloadsUnique = new TreeSet<>(payloads);
        System.out.println(Joiner.on("\n").join(payloadsUnique));

        Long[] pq = payloadsUnique.toArray(new Long[0]);

        printDifferingBits(pq);
    }

    private static void printDifferingBits(Long[] pq) {
        for (int i = 0; i < pq.length; i++) {
            for (int j = i+1; j < pq.length; j++) {
                System.out.println("Comparing " + pq[i] + " with " + pq[j]);
                final long delta = pq[i] ^ pq[j];
                System.out.println("Differing bit positions: " + Joiner.on(",").join(bitPositions(delta)));
            }
        }
    }

    private static List<Integer> bitPositions(long number) {
        List<Integer> positions = Lists.newArrayList();
        int pos = 1;
        while (number != 0) {
            if ((number & 1) != 0) {
                positions.add(pos);
            }
            pos++;
            number >>>= 1;
        }
        return positions;
    }
}