package net.pisarenko.pcv.tools;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;
import net.pisarenko.pcv.comm.PacketReceiver;
import net.pisarenko.pcv.common.Packet;
import net.pisarenko.pcv.values.RPM;
import net.pisarenko.pcv.values.Throttle;

import java.time.format.DateTimeFormatter;
import java.util.Queue;

/**
 * Establish a USB connection with Power Commander 5.
 */
public class PcvUsbConnect {
    private static final int REFRESH_FREQUENCY_MILLIS = 0;

    public static void main(String[] args) throws Exception {
        Queue<Packet> queue = Queues.synchronizedQueue(EvictingQueue.<Packet>create(1000));

        new Thread(new PacketReceiver(REFRESH_FREQUENCY_MILLIS, queue)).start();
        new Thread(new PacketPrinter(queue)).start();
    }

    @SuppressWarnings("squid:S2189")
    private static class PacketPrinter implements Runnable {
        private Queue<Packet> queue;

        public PacketPrinter(Queue<Packet> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                if (!queue.isEmpty()) {
                    Packet packet = queue.poll();
                    System.out.println(
                            DateTimeFormatter.ISO_DATE_TIME.format(packet.getTimestamp()) + " " +
                                    "THROTTLE: " + Throttle.fromPacket(packet) +
                                    " RPM: " + RPM.fromPacket(packet));
                }
            }
        }
    }
}
