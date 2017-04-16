package net.pisarenko.pcv.comm;

import net.pisarenko.pcv.common.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import java.util.Optional;
import java.util.Queue;

@SuppressWarnings("squid:S2189")
public class PacketReceiver implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketReceiver.class);

    private static final int RECEIVE_RETRY_COUNT = 20;

    /** How often should we request new values. */
    private int frequency;
    /** Where the received packets go. */
    private Queue<Packet> queue;

    public PacketReceiver(final int frequency, final Queue<Packet> queue) {
        this.frequency = frequency;
        this.queue = queue;
    }

    @Override
    public void run() {
        Optional<USBConnection> connectionOpt;
        USBConnection connection;
        Packet receivedPacket, sendPacket;

        try {
            // main loop
            while (true) {
                // keep trying to open a connection
                connectionOpt = Optional.empty();
                while (!connectionOpt.isPresent()) {
                    try {
                        LOGGER.debug("Trying to establish USB connection.");
                        connectionOpt = USBConnection.establish();
                    } catch (UsbException e) {
                        LOGGER.debug("Failed to open USB connection", e);
                    }

                    Thread.sleep(2000);
                }
                LOGGER.info("USB connection established.");

                // send and receive messages
                connection = connectionOpt.get();
                while (true) {
                    try {
                        sendPacket = Packet.createStatsRequestPacket();
                        connection.sendPacket(sendPacket);
                        int retryCount = 0;

                        do {
                            retryCount++;
                            receivedPacket = connection.receivePacket();
                        } while (receivedPacket.getId() != sendPacket.getId() && retryCount < RECEIVE_RETRY_COUNT);

                        queue.add(receivedPacket);
                    } catch (UsbDisconnectedException e) {
                        LOGGER.info("USB connection lost");
                        break;
                    } catch (UsbException e) {
                        LOGGER.debug("Exception occurred when sending/receiving", e);
                    }

                    Thread.sleep(frequency);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}