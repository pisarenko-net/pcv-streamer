package net.pisarenko.pcv.comm;

import net.pisarenko.pcv.common.Command;
import net.pisarenko.pcv.common.Packet;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("squid:S2189")
public class PacketReceiver implements Runnable {
    private static final byte[] REQUEST_PACKET_PAYLOAD = new byte[]{
            (byte)0x1b, (byte)0x1c, (byte)0x2a, (byte)0x2e, (byte)0xc5,
            (byte)0x8f, (byte)0xc3, (byte)0x1d, (byte)0x1f, (byte)0x8e,
            (byte)0xe0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
    };

    private static final int RECEIVE_RETRY_COUNT = 20;

    /** How often should we request new values. */
    private int frequency;
    /** Where the received packets go. */
    private List<Packet> queue;

    public PacketReceiver(final int frequency, final List<Packet> queue) {
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
                        connectionOpt = USBConnection.establish();
                    } catch (UsbException e) {
                        e.printStackTrace();
                    }

                    System.out.println("FAILED TO ESTABLISH USB CONNECTION, RETRYING");
                    Thread.sleep(2000);
                }
                System.out.println("USB CONNECTION ESTABLISHED");

                // send and receive messages
                connection = connectionOpt.get();
                while (true) {
                    try {
                        sendPacket = Packet.createSendPacket(Command.GET_CHANNEL_STATUS, REQUEST_PACKET_PAYLOAD);
                        connection.sendPacket(sendPacket);
                        int retryCount = 0;

                        do {
                            retryCount++;
                            receivedPacket = connection.receivePacket();
                        } while (receivedPacket.getId() != sendPacket.getId() && retryCount < RECEIVE_RETRY_COUNT);

                        queue.add(receivedPacket);
                    } catch (UsbDisconnectedException e) {
                        System.out.println("LOST USB CONNECTION");
                        break;
                    } catch (UsbException e) {
                        System.out.println("EXCEPTION OCCURRED: " + e);
                    }

                    Thread.sleep(frequency);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}