package net.pisarenko.pcv.comm;

import net.pisarenko.pcv.common.Command;
import net.pisarenko.pcv.common.Packet;
import net.pisarenko.pcv.common.PacketUtil;
import net.pisarenko.pcv.values.RPM;
import net.pisarenko.pcv.values.Throttle;

import javax.usb.*;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Establish a USB connection with Power Commander 5.
 */
public class PcvUsbConnect {
    public static final int VENDOR_ID = 0x10b6;
    public static final int PRODUCT_ID = 0x0502;

    public static final byte FROM_DEVICE_ENDPOINT_ADDRESS = (byte) 0x81;
    public static final byte TO_DEVICE_ENDPOINT_ADDRESS = (byte) 0x01;

            //00000000   82 00 5a 01 02 21 09 00 c0 00 00 00 40 00 00 00    ..Z..!......@...
            //00000010   06 00 00 00 06 00 00 00 06 00 00 00 06 00 00 00    ................
            //00000020   06 00 00 00 c0 33 e6 00 c0 33 e6 00 1c 00 00 00    .....3...3......
            //00000030   06 00 00 00 06 00 00 00 06 00 00 00 44 00 00 00    ............D...

    private final static int[] CONNECTION_INIT_PACKET = new int[]{
            0x82, 0x00, 0x5a, 0x01, 0x02, 0x21, 0x09, 0x00, 0xC0, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00,
            0x06, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00,
            0x06, 0x00, 0x00, 0x00, 0xc0, 0x33, 0xe6, 0x00, 0xc0, 0x33, 0xe6, 0x00, 0x1c, 0x00, 0x00, 0x00,
            0x06, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x44, 0x00, 0x00, 0x00,
    };

    private final static int[] REQUEST_STATS_PACKET = new int[]{
            0x0d, 0x0b, 0x18, 0x6e, 0x05, 0x00, 0x0f, 0x00, 0x1b, 0x1c, 0x2a, 0x2e, 0xc5, 0x8f, 0xc3, 0x1d,
            0x1f, 0x8e, 0xe0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    /**
     * USB library works like the following. First, create an async pipe listener. Then, create an empty buffer (64 byte)
     * and make an asyncSubmit with it. Once the device provides a response the
     */
    public static void main(String[] args) throws Exception {
        UsbDevice pcv = findPcv(UsbHostManager.getUsbServices().getRootUsbHub());
        if (pcv == null) {
            System.out.println("FAILED to find PCV. Exiting.");
            return;
        }

        final Clock clock = Clock.systemUTC();

        Random random = new Random();

        UsbConfiguration configuration = pcv.getActiveUsbConfiguration();

        UsbInterface iface = configuration.getUsbInterface((byte)0);
        iface.claim(usbInterface -> true);

        for (UsbEndpoint endpoint : (List<UsbEndpoint>) iface.getUsbEndpoints()) {
            System.out.println("ENDPOINT type: " + endpoint.getType());
            System.out.println("ENDPOINT direction: " + endpoint.getDirection());
            System.out.println("ENDPOINT descriptor: " + endpoint.getUsbEndpointDescriptor());
        }

        final UsbEndpoint fromDevice = iface.getUsbEndpoint(FROM_DEVICE_ENDPOINT_ADDRESS);
        final UsbEndpoint toDevice = iface.getUsbEndpoint(TO_DEVICE_ENDPOINT_ADDRESS);

        System.out.println(fromDevice);
        System.out.println(toDevice);

        toDevice.getUsbPipe().open();
        fromDevice.getUsbPipe().open();

        fromDevice.getUsbPipe().addUsbPipeListener(new UsbPipeListener() {
            @Override
            public void errorEventOccurred(UsbPipeErrorEvent usbPipeErrorEvent) {
                System.out.println("ERROR OCCURRED ON RECEIVE: " + usbPipeErrorEvent);
            }

            @Override
            public void dataEventOccurred(UsbPipeDataEvent usbPipeDataEvent) {
                final Packet packet = new Packet(usbPipeDataEvent.getData(), Packet.PacketDirection.UP, 0, LocalDateTime.now(clock));
                if (packet.getCommand() == Command.GET_CHANNEL_STATUS) {
                    System.out.println("[FROM_DEVICE] RECEIVED DATA: " + Arrays.toString(PacketUtil.bytesToHexStrings(packet.getRawPacket())));
                    System.out.println("THROTTLE: " + Throttle.fromPacket(packet));
                    System.out.println("RPM: " + RPM.fromPacket(packet));
                }
            }
        });

        System.out.println("SENDING INIT MESSAGE " + Arrays.toString(intsToBytes(CONNECTION_INIT_PACKET)));
        toDevice.getUsbPipe().syncSubmit(intsToBytes(CONNECTION_INIT_PACKET));

        byte[] data = new byte[64];
        fromDevice.getUsbPipe().syncSubmit(data);
        System.out.println("RECEIVED: " + Arrays.toString(data));

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(400);
                    toDevice.getUsbPipe().asyncSubmit(intsToBytes(REQUEST_STATS_PACKET));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            while (true) {
                try {
                    byte[] buffer = new byte[64];
                    fromDevice.getUsbPipe().asyncSubmit(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

//        while (1 == 1) {
//            Thread.sleep(100);
//            data = new byte[64];
//            fromDevice.getUsbPipe().asyncSubmit(data);
            //System.out.println("SENDING REQUEST: " + Arrays.toString(intsToBytes(REQUEST_STATS_PACKET)));

            //System.out.println("RECEIVED: " + Arrays.toString(data));
//
//            System.out.println("SENDING REQUEST: " + Arrays.toString(intsToBytes(REQUEST_STATS_PACKET)));
//            toDevice.getUsbPipe().syncSubmit(intsToBytes(REQUEST_STATS_PACKET));
//
//            data = new byte[64];
//            fromDevice.getUsbPipe().syncSubmit(data);
//            System.out.println("RECEIVED: " + Arrays.toString(data));
//        }

//        data = new byte[64];
//        fromDevice.getUsbPipe().syncSubmit(data);
//        System.out.println("RECEIVED: " + Arrays.toString(data));
//
//        data = new byte[64];
//        fromDevice.getUsbPipe().syncSubmit(data);
//        System.out.println("RECEIVED: " + Arrays.toString(data));
//
//        data = new byte[64];
//        fromDevice.getUsbPipe().syncSubmit(data);
//        System.out.println("RECEIVED: " + Arrays.toString(data));
//
//        data = new byte[64];
//        fromDevice.getUsbPipe().syncSubmit(data);
//        System.out.println("RECEIVED: " + Arrays.toString(data));
//
//        data = new byte[64];
//        fromDevice.getUsbPipe().syncSubmit(data);
//        System.out.println("RECEIVED: " + Arrays.toString(data));




//
//        toDevice.getUsbPipe().addUsbPipeListener(new UsbPipeListener() {
//            @Override
//            public void errorEventOccurred(UsbPipeErrorEvent usbPipeErrorEvent) {
//                System.out.println("ERROR OCCURRED ON RECEIVE: " + usbPipeErrorEvent);
//            }
//
//            @Override
//            public void dataEventOccurred(UsbPipeDataEvent usbPipeDataEvent) {
//                final Packet packet = new Packet(usbPipeDataEvent.getData(), Packet.PacketDirection.UP, 0, LocalDateTime.now(clock));
//                System.out.println("[TO_DEVICE] RECEIVED DATA: " + Arrays.toString(PacketUtil.bytesToHexStrings(packet.getRawPacket())));
//                System.out.println("PAYLOAD DATA: " + Arrays.toString(PacketUtil.bytesToHexStrings(packet.getRawPayload())));
//                System.out.println("THROTTLE: " + Throttle.fromPacket(packet));
//                System.out.println("RPM: " + RPM.fromPacket(packet));
//            }
//        });
//
//        while (1 == 1) {
//            Thread.sleep(2000);
//            byte[] request = intsToBytes(REQUEST_STATS_PACKET);
//            int randInt = random.nextInt();
//            request[0] = (byte)(randInt & 0xFF);
//            request[1] = (byte)((randInt >> 8) & 0xFF);
//            request[2] = (byte)((randInt >> 16) & 0xFF);
//            request[3] = (byte)((randInt >> 24) & 0xFF);
//            System.out.println("REQUESTING INFO: " + Arrays.toString(request))
//            toDevice.getUsbPipe().asyncSubmit(request);
//        }
    }

    private static byte[] intsToBytes(final int[] ints) {
        byte[] bytes = new byte[64];
        for (int i = 0; i < 64; i++) {
            bytes[i] = (byte)ints[i];
        }
        return bytes;
    }

    private static UsbDevice findPcv(final UsbHub rootHub) throws Exception {
        UsbDevice pcv;

        for (UsbDevice device: (List<UsbDevice>) rootHub.getAttachedUsbDevices()) {
            System.out.println("ATTACHED: " + device.getManufacturerString() + " " + device.getProductString());
            if (device.isUsbHub()) {
                pcv = findPcv((UsbHub) device);
                if (pcv != null) {
                    return pcv;
                }
            } else {
                UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
                if (desc.idVendor() == VENDOR_ID && desc.idProduct() == PRODUCT_ID) {
                    return device;
                }
            }
        }
        return null;
    }
}
