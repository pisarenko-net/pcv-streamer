package net.pisarenko.pcv.comm;

import javax.usb.*;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;
import java.util.Arrays;
import java.util.List;

/**
 * Establish a USB connection with Power Commander 5.
 */
public class PcvUsbConnect {
    public static final int VENDOR_ID = 0x10b6;
    public static final int PRODUCT_ID = 0x0502;

    private final static int[] SEND_MESSAGE = new int[]{
            0x0d, 0x0b, 0x18, 0x6e, 0x05, 0x00, 0x0f, 0x00, 0x1b, 0x1c, 0x2a, 0x2e, 0xc5, 0x8f, 0xc3, 0x1d,
            0x1f, 0x8e, 0xe0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    public static void main(String[] args) throws Exception {
        UsbDevice pcv = findPcv(UsbHostManager.getUsbServices().getRootUsbHub());
        if (pcv == null) {
            System.out.println("FAILED to find PCV. Exiting.");
            return;
        }

        UsbConfiguration configuration = pcv.getActiveUsbConfiguration();
        UsbInterface iface = configuration.getUsbInterface((byte)0);
        iface.claim();
        final UsbEndpoint in = iface.getUsbEndpoint((byte)0);
        final UsbEndpoint out = iface.getUsbEndpoint((byte)1);

        in.getUsbPipe().addUsbPipeListener(new UsbPipeListener() {
            @Override
            public void errorEventOccurred(UsbPipeErrorEvent usbPipeErrorEvent) {
                System.out.println("ERROR OCCURRED ON RECEIVE: " + usbPipeErrorEvent);
            }

            @Override
            public void dataEventOccurred(UsbPipeDataEvent usbPipeDataEvent) {
                System.out.println("RECEIVED DATA: " + Arrays.toString(usbPipeDataEvent.getData()));
            }
        });

        in.getUsbPipe().open();
        out.getUsbPipe().open();

        while (1 == 1) {
            Thread.sleep(2000);
            out.getUsbPipe().asyncSubmit(intsToBytes(SEND_MESSAGE));
        }
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
