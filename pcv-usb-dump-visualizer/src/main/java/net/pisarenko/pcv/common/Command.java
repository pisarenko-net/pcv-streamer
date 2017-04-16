package net.pisarenko.pcv.common;

/**
 * Commands that are supported by the PCV.
 */
public enum Command  {
    INVALID(0),
    /** This command is sent regularly from the device even when no commands are sent to it. */
    CAN_PASS(8450),
    /** The command relevant to reading out real-time engine values, such as RPMs, throttle, gear. */
    GET_CHANNEL_STATUS(5),
    READ_FLASH(6),
    CLEAR_ERRORS(20481),
    ENTER_EDIT_MODE(20482),
    EXIT_EDIT_MODE(20483),
    GET_BOOT_INFO(234),
    GET_FIRMWARE_INFO(235),
    UPDATE_DEVICE(20480),
    READ_FRAM(3),
    WRITE_FRAM(4);

    private int command;

    Command(final int command) {
        this.command = command;
    }

    public static Command fromInt(final int n) {
        switch (n) {
            default: {
                return Command.INVALID;
            }
            case 3: {
                return Command.READ_FRAM;
            }
            case 4: {
                return Command.WRITE_FRAM;
            }
            case 5: {
                return Command.GET_CHANNEL_STATUS;
            }
            case 6: {
                return Command.READ_FLASH;
            }
            case 234: {
                return Command.GET_BOOT_INFO;
            }
            case 235: {
                return Command.GET_FIRMWARE_INFO;
            }
            case 8450: {
                return Command.CAN_PASS;
            }
            case 20480: {
                return Command.UPDATE_DEVICE;
            }
            case 20481: {
                return Command.CLEAR_ERRORS;
            }
            case 20482: {
                return Command.ENTER_EDIT_MODE;
            }
            case 20483: {
                return Command.EXIT_EDIT_MODE;
            }
        }
    }

    public int toValue() {
        return this.command;
    }
}