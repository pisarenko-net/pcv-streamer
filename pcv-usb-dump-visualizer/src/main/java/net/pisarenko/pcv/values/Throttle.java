package net.pisarenko.pcv.values;

import net.pisarenko.pcv.common.Message;

/**
 * Throttle value is stored in 2 bytes starting from byte 9. Value ranges from 1 to 1000.
 *
 * For example, in the following payload throttle value is stored in 95 01:
 * [01 00 00 00 00 00 00 00 00 95 01 13 00 00 00 00 00 00 01 00 00 00 00 00 00 6D 04 00 00]
 *
 * To get value 405 reverse the bytes (0x0195) and convert to decimal.
 */
public class Throttle {
    private Throttle() {}

    public static int fromMessage(final Message message) {
        return (int) message.getPayloadFragment(9, 2);
    }
}
