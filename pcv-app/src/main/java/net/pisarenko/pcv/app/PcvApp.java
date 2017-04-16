package net.pisarenko.pcv.app;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;
import net.pisarenko.pcv.comm.PacketReceiver;
import net.pisarenko.pcv.common.Packet;
import net.pisarenko.pcv.streamer.AmazonMqttStreamer;
import net.pisarenko.pcv.streamer.StreamerMessage;
import net.pisarenko.pcv.values.RPM;
import net.pisarenko.pcv.values.Throttle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

public class PcvApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(PcvApp.class);

    /** Max queue size: when Internet connection is lost we won't buffer more than X messages. */
    private static final int MAX_QUEUE_SIZE = 2000;
    /** How often to request values from the PCV. */
    private static final int UPDATE_FREQUENCY_MILLIS = 100;

    /** Path to the Amazon root CA. */
    private static String ROOT_CA_PATH = "rootCA.crt";
    /** Path to the certificate (generated during setup). */
    private static String CERT_PATH = "cert.pem";
    /** Path to the private key file (generated during setup). */
    private static String PRIVATE_KEY_PATH = "privkey.pem";
    private static String SERVER_URL = "ssl://data.iot.eu-west-1.amazonaws.com:8883";
    private static String CLIENT_ID = "KTMDuke390";

    public static void main(String[] args) {
        Queue<Packet> packetQueue = Queues.synchronizedQueue(EvictingQueue.<Packet>create(MAX_QUEUE_SIZE));
        Queue<StreamerMessage> streamerQueue = Queues.synchronizedQueue(EvictingQueue.<StreamerMessage>create(MAX_QUEUE_SIZE));

        // fetches fresh data from the PCV over USB
        new Thread(new PacketReceiver(UPDATE_FREQUENCY_MILLIS, packetQueue)).start();
        // transform data from USB to JSON messages for Amazon consumption
        new Thread(new PacketToAmazonMessage(streamerQueue, packetQueue)).start();
        // sends data to Amazon
        new Thread(new AmazonMqttStreamer(SERVER_URL, CLIENT_ID, streamerQueue, ROOT_CA_PATH, CERT_PATH, PRIVATE_KEY_PATH)).start();
    }

    @SuppressWarnings("squid:S2189")
    private static class PacketToAmazonMessage implements Runnable {
        private Queue<StreamerMessage> amazonQueue;
        private Queue<Packet> packetQueue;

        public PacketToAmazonMessage(Queue<StreamerMessage> amazonQueue, Queue<Packet> packetQueue) {
            this.amazonQueue = amazonQueue;
            this.packetQueue = packetQueue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (!packetQueue.isEmpty()) {
                        Packet packet = packetQueue.poll();
                        int rpm = RPM.fromPacket(packet);
                        int throttle = Throttle.fromPacket(packet);
                        amazonQueue.add(new StreamerMessage("RPM", "" + rpm, packet.getTimestamp()));
                        amazonQueue.add(new StreamerMessage("Throttle", "" + throttle, packet.getTimestamp()));
                    } else {
                        Thread.sleep(50);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
