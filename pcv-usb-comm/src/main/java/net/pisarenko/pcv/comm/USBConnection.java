package net.pisarenko.pcv.comm;

import net.pisarenko.pcv.common.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.usb.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class USBConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(USBConnection.class);

    private static final int VENDOR_ID = 0x10b6;
    private static final int PRODUCT_ID = 0x0502;

    private static final byte FROM_DEVICE_ENDPOINT_ADDRESS = (byte) 0x81;
    private static final byte TO_DEVICE_ENDPOINT_ADDRESS = (byte) 0x01;

    private UsbPipe toDevice;
    private UsbPipe fromDevice;

    private USBConnection() {}

    public static Optional<USBConnection> establish() throws UsbException {
        UsbDevice device = findUSBDevice(UsbHostManager.getUsbServices().getRootUsbHub());
        if (device == null) {
            LOGGER.debug("PCV USB device not found");
            return Optional.empty();
        }

        USBConnection connection = new USBConnection();

        UsbConfiguration configuration = device.getActiveUsbConfiguration();
        UsbInterface iface = configuration.getUsbInterface((byte)0);
        iface.claim(usbInterface -> true);

        connection.fromDevice = iface.getUsbEndpoint(FROM_DEVICE_ENDPOINT_ADDRESS).getUsbPipe();
        connection.fromDevice.open();

        connection.toDevice = iface.getUsbEndpoint(TO_DEVICE_ENDPOINT_ADDRESS).getUsbPipe();
        connection.toDevice.open();

        return Optional.of(connection);
    }

    public void sendPacket(final Packet packet) throws UsbException {
        toDevice.syncSubmit(packet.getRawPacket());
    }

    public Packet receivePacket() throws UsbException {
        final byte[] buffer = new byte[64];
        fromDevice.syncSubmit(buffer);
        return Packet.createFromReceivedData(buffer, LocalDateTime.now(Clock.systemUTC()));
    }

    private static UsbDevice findUSBDevice(final UsbHub rootHub) {
        UsbDevice pcv;

        for (UsbDevice device: (List<UsbDevice>) rootHub.getAttachedUsbDevices()) {
            if (device.isUsbHub()) {
                pcv = findUSBDevice((UsbHub) device);
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
