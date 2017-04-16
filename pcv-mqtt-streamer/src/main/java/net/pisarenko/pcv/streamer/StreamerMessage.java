package net.pisarenko.pcv.streamer;

import java.time.LocalDateTime;

public class StreamerMessage {
    private String topic;
    private String value;
    private LocalDateTime timestamp;

    public StreamerMessage(String topic, String value, LocalDateTime timestamp) {
        this.topic = topic;
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getTopic() {
        return topic;
    }

    public String getValue() {
        return value;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}