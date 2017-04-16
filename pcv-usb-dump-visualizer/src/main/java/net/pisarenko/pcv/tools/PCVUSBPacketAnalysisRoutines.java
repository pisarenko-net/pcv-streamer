package net.pisarenko.pcv.tools;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import net.pisarenko.pcv.common.Command;
import net.pisarenko.pcv.common.Packet;
import net.pisarenko.pcv.common.PacketUtil;
import net.pisarenko.pcv.values.RPM;
import net.pisarenko.pcv.values.Throttle;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This program parses text output captured from a PCV using the HHD Device Monitoring Studio Filter.
 *
 * The goal is to understand where are the useful data bits stored. The program is based on previous discoveries and
 * many assumptions.
 *
 * This is of use only when trying to gain new insight from existing USB dumps.
 */
public class PCVUSBPacketAnalysisRoutines {
    public static void main(String[] args) throws Exception {
        final String usbDumpPath = args[0];
        final List<Packet> packets = PacketUtil.parseUSBPackets(usbDumpPath);
        final String routine = args[1];

        switch (routine) {
            case "all":
                packets.forEach(System.out::println);
                break;
            case "up":
                printChannelStatusMessages(packets, Packet.PacketDirection.UP);
                break;
            case "down":
                printChannelStatusMessages(packets, Packet.PacketDirection.DOWN);
                break;
            case "1":
                analyzeDifferingBitsInUpChannelStatusMessages(packets);
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
                analyzeAllegedThrottleValues(packets);
                break;
            case "4":
                analyzeAllegedRPMValues(packets);
                break;
            case "create":
                createPacketAndPrint();
                break;
            default:
                System.out.println("Unknown routine");
        }
    }

    private static void createPacketAndPrint() {
        final Packet packet = Packet.createSendPacket(Command.GET_CHANNEL_STATUS, new byte[]{
                (byte)0x1b, (byte)0x1c, (byte)0x2a, (byte)0x2e, (byte)0xc5,
                (byte)0x8f, (byte)0xc3, (byte)0x1d, (byte)0x1f, (byte)0x8e,
                (byte)0xe0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        });
        System.out.println(packet);
    }

    private static void printChannelStatusMessages(final List<Packet> packets, Packet.PacketDirection direction) {
        packets.stream()
                .filter(m -> m.getDirection() == direction)
                .filter(m -> m.getCommand() == Command.GET_CHANNEL_STATUS)
                .forEach(System.out::println);
    }

    private static void analyzeAllegedThrottleValues(final List<Packet> packets) {
        packets.stream()
                .filter(m -> m.getDirection() == Packet.PacketDirection.UP)
                .filter(m -> m.getCommand() == Command.GET_CHANNEL_STATUS)
                .filter(m -> m.getPayloadLength() == 29)
                .forEach(m -> System.out.println("[" + Joiner.on(" ").join(PacketUtil.bytesToHexStrings(m.getRawPayload())) + "] " + Throttle.fromPacket(m)));
    }

    private static void analyzeAllegedRPMValues(final List<Packet> packets) {
        packets.stream()
                .filter(m -> m.getDirection() == Packet.PacketDirection.UP)
                .filter(m -> m.getCommand() == Command.GET_CHANNEL_STATUS)
                .filter(m -> m.getPayloadLength() == 29)
                .forEach(m -> System.out.println("[" + Joiner.on(" ").join(PacketUtil.bytesToHexStrings(m.getRawPayload())) + "] " + RPM.fromPacket(m)));
    }

    /**
     * Take unique values of the "get channel status" command and see in what bits do they differ.
     */
    private static void analyzeDifferingBitsInUpChannelStatusMessages(final List<Packet> packets) {
        List<Long> payloads = packets.stream()
                .filter(packet -> packet.getDirection()== Packet.PacketDirection.UP)
                .filter(packet -> packet.getCommand() == Command.GET_CHANNEL_STATUS)
                .map(Packet::getPayloadAsLong).collect(Collectors.toList());

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