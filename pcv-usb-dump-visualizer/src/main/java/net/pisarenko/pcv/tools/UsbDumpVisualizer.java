package net.pisarenko.pcv.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

/**
 * This program follows a mistaken assumption - TransferBuffer is not the actual data. See {@link PCVUSBMessageAnalysisRoutines}
 * instead.
 */
public class UsbDumpVisualizer {
    public static void main(String[] args) throws Exception {
        final String jsonPath = args[0];
        // could be: Down, Up, BOTH
        final String directionFilter = args[1];
        // replace provided buffer values with XXX
        Set<String> bufferFilter = new HashSet<>();
        if (args.length > 2) {
            bufferFilter.addAll(Arrays.asList(args[2].split(",")));
        }

        final ObjectMapper objectMapper = new ObjectMapper();
        final File input = new File(jsonPath);
        final ArrayNode nodes = objectMapper.readValue(input, ArrayNode.class);

        final Multiset<String> countBufferValues = HashMultiset.create();

        for (JsonNode node : nodes) {
            final String time = node.get("time").textValue();
            final String dataDirection = node.get("data_dir").textValue();
            // "down" means from host to device, "up" from device to host
            final String direction = node.get("dir").textValue();
            final JsonNode command = node.get("command");

            if (!command.isNull()
                    && command.has("UrbBulkOrInterruptTransfer")
                    && command.get("UrbBulkOrInterruptTransfer").has("TransferBuffer")) {

                final JsonNode transferBuffer = command.get("UrbBulkOrInterruptTransfer").get("TransferBuffer");
                final JsonNode pipeHandle = command.get("UrbBulkOrInterruptTransfer").get("PipeHandle");
                final String buffer = transferBuffer.get("to_HCD").textValue();

                if (!Strings.isNullOrEmpty(buffer) && isRightDirection(directionFilter, direction)) {
                    countBufferValues.add(buffer);
                } else {
                    continue;
                }

                final String pipe = pipeHandle.get("to_HCD").textValue();

                if (!bufferFilter.contains(buffer)) {
                    System.out.println(
                            format("[%s] %s BUFFER: %s, PIPE: %s [%s]",
                                    time, direction.charAt(0), buffer, pipe, dataDirection));
                } else {
                    System.out.println(format("[%s] XXX", time));
                }
            }
        }

        System.out.println("Total buffer  values count: " + countBufferValues.size());
        System.out.println("Unique buffer values count: " + countBufferValues.elementSet().size());
        System.out.println("Values: " + Joiner.on(", ").join(countBufferValues.elementSet()));
        System.out.println("Value distribution:");
        for (Multiset.Entry<String> bufferEntry : countBufferValues.entrySet()) {
            System.out.println(bufferEntry.getElement() + ": " + bufferEntry.getCount());
        }
    }

    private static boolean isRightDirection(final String directionFilter, final String direction) {
        return directionFilter.equals("BOTH") || direction.equals(directionFilter);
    }
}
