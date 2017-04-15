package net.pisarenko.pcv.values;

import net.pisarenko.pcv.common.Message;

/**
 * RPM value is stored in 2 bytes starting from byte 1.
 *
 * For example, in the following payload the value is in 9C 11:
 * [01 9C 11 C5 02 20 03 00 00 1C 00 17 00 00 00 00 00 00 01 00 00 00 00 00 00 6D 04 00 00]
 *
 * To obtain value 4508 from this reverse bytes (0x119C) and convert to decimal.
 */
public class RPM {
    private RPM() {}

    public static int fromMessage(final Message message) {
        return (int) message.getPayloadFragment(1, 2);
    }
}
