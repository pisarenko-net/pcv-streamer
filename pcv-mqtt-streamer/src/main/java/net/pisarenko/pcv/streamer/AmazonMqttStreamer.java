package net.pisarenko.pcv.streamer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.Queue;

import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_NOT_CONNECTED;

@SuppressWarnings("squid:S2189")
public class AmazonMqttStreamer implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonMqttStreamer.class);

    private static ObjectMapper mapper = new ObjectMapper();

    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final int CONNECTION_TIMEOUT_SECONDS = 0;

    private String serverUrl;
    private String clientId;
    private Queue<StreamerMessage> queue;

    private String rootCaPath;
    private String certPath;
    private String privateKeyPath;

    public AmazonMqttStreamer(
            final String serverUrl, final String clientId, final Queue<StreamerMessage> queue,
            final String rootCaPath, final String certPath, final String privateKeyPath) {
        this.serverUrl = serverUrl;
        this.clientId = clientId;
        this.queue = queue;

        this.rootCaPath = rootCaPath;
        this.certPath = certPath;
        this.privateKeyPath = privateKeyPath;
    }

    public void run() {
        MqttClient client = null;
        StreamerMessage streamerMessage;

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setConnectionTimeout(CONNECTION_TIMEOUT_SECONDS);
        connOpts.setSocketFactory(SslUtil.getSocketFactory(rootCaPath, certPath, privateKeyPath, ""));
        connOpts.setCleanSession(true);

        try {
            // main loop
            while (true) {
                // establish or re-establish connection loop
                do {
                    LOGGER.info("Trying to establish connection to Amazon");

                    try {
                        client = new MqttClient(serverUrl, clientId, new MemoryPersistence());
                        client.connect(connOpts);
                        LOGGER.info("Connection to Amazon established");
                    } catch (MqttException e) {
                        LOGGER.debug("Failed to connect", e);
                    }

                    Thread.sleep(2000);
                } while (client == null || !client.isConnected());

                // send message loop
                while (true) {
                    try {
                        if (!queue.isEmpty()) {
                            streamerMessage = queue.peek();
                            String messageJson = createJsonString(streamerMessage);
                            MqttMessage message = new MqttMessage(messageJson.getBytes());
                            client.publish(streamerMessage.getTopic(), message);
                            queue.remove();
                            LOGGER.debug("Sent " + streamerMessage.getTopic() + " " + streamerMessage.getValue() + " " + DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT).format(streamerMessage.getTimestamp()));
                        } else {
                            Thread.sleep(50);
                        }
                    } catch (MqttException e) {
                        if (e.getReasonCode() == REASON_CODE_CLIENT_NOT_CONNECTED) {
                            break;
                        } else {
                            LOGGER.debug("Exception occurred when sending a message", e);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String createJsonString(StreamerMessage streamerMessage) {
        final ObjectNode node = mapper.createObjectNode();
        node.put("type", streamerMessage.getTopic());
        node.put("value", streamerMessage.getValue());
        node.put("timestamp", DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT).format(streamerMessage.getTimestamp()));
        return node.toString();
    }
}
