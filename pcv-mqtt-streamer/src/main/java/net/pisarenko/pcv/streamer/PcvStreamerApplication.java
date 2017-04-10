package net.pisarenko.pcv.streamer;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class PcvStreamerApplication {

    /** Path to the Amazon root CA. */
    private static String ROOT_CA_PATH = "rootCA.crt";
    /** Path to the certificate (generated during setup). */
    private static String CERT_PATH = "cert.pem";
    /** Path to the private key file (generated during setup). */
    private static String PRIVATE_KEY_PATH = "privkey.pem";

    public static void main(String[] args) {
        String topic        = "KTM Duke 390 RPM";
        String serverUrl    = "ssl://data.iot.eu-west-1.amazonaws.com:8883";
        String clientId     = "KTMDuke390";

        String content      = "RPM read-out from the engine";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient sampleClient = new MqttClient(serverUrl, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setSocketFactory(SslUtil.getSocketFactory(
                    ROOT_CA_PATH, CERT_PATH, PRIVATE_KEY_PATH, ""));
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+serverUrl);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            System.out.println("Publishing message: "+content);
            MqttMessage message = new MqttMessage(content.getBytes());
            sampleClient.publish(topic, message);
            System.out.println("Message published");
            sampleClient.disconnect();
            System.out.println("Disconnected");
            System.exit(0);
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
}
