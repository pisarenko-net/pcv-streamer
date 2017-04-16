package net.pisarenko.pcv.streamer;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.Random;

public class PcvStreamerApplication {
    /** Path to the Amazon root CA. */
    private static String ROOT_CA_PATH = "rootCA.crt";
    /** Path to the certificate (generated during setup). */
    private static String CERT_PATH = "cert.pem";
    /** Path to the private key file (generated during setup). */
    private static String PRIVATE_KEY_PATH = "privkey.pem";
    private static String SERVER_URL = "ssl://data.iot.eu-west-1.amazonaws.com:8883";
    private static String CLIENT_ID = "KTMDuke390";

    public static void main(String[] args) throws Exception {
        Queue<StreamerMessage> queue = Queues.synchronizedQueue(EvictingQueue.<StreamerMessage>create(1000));

        new Thread(new AmazonMqttStreamer(SERVER_URL, CLIENT_ID, queue, ROOT_CA_PATH, CERT_PATH, PRIVATE_KEY_PATH)).start();
        new Thread(new ValueGenerator(queue)).start();
    }

    @SuppressWarnings("squid:S2189")
    private static class ValueGenerator implements Runnable {
        private Queue<StreamerMessage> queue;
        private Random random = new Random();

        public ValueGenerator(final Queue<StreamerMessage> queue) {
            this.queue = queue;
        }

        public void run() {
            try {
                while (true) {
                    queue.add(new StreamerMessage("RPM", "" + random.nextInt(100), LocalDateTime.now(Clock.systemUTC())));
                    Thread.sleep(3000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
