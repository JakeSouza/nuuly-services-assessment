package com.nuuly;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


/**
 * This is the service class for sending messages to Kafka topics.
 */
@Service
public class KafkaProducerService<T> {

    /**
     * The Kafka template for sending messages.
     */
    private final KafkaTemplate<String, T> kafkaTemplate;

    /**
     * Constructor for the KafkaProducerService.
     * @param kafkaTemplate The Kafka template for sending messages.
     */
    @Autowired
    public KafkaProducerService(KafkaTemplate<String, T> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a message to the specified Kafka topic.
     * @param topic The Kafka topic to send the message to.
     * @param key The key for the message.
     * @param value The value for the message.
     * @throws Exception If an error occurs while sending the message.
     */
    public void sendMessage(String topic, String key, T value) throws Exception {
        ProducerRecord<String, T> producerRecord = producerRecord(topic, key, value);
        int PRODUCER_TIMEOUT = 30000;
        kafkaTemplate.send(producerRecord).get(PRODUCER_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a ProducerRecord with the specified topic, key, and value.
     * @param topic The Kafka topic for the ProducerRecord.
     * @param key The key for the ProducerRecord.
     * @param value The value for the ProducerRecord.
     * @return A ProducerRecord with the specified topic, key, and value.
     */
    private ProducerRecord<String, T> producerRecord(String topic, String key, T value) {
        return new ProducerRecord<>(topic, key, value);
    }
}