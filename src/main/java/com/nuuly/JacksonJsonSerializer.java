package com.nuuly;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Kafka value serializer that serializes objects to JSON bytes using Jackson.
 * Use this instead of the deprecated {@code org.springframework.kafka.support.serializer.JsonSerializer}.
 */
public class JacksonJsonSerializer implements Serializer<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // no-op
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error serializing to JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        // no-op
    }
}
