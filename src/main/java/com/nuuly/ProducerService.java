package com.nuuly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This is an example ProducerService that can publish a message to a topic. This can be used if it is autowired to
 * another class.
 * An example of using this is as follows:
 *
 *      <pre>
 *      this.producer.sendInventoryMessage(
 *          key = key,
 *          value = value
 *     )
 *     </pre>
 */
@Service
public class ProducerService {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    private final KafkaProducerService<String> kafkaProducerService;

    final String INVENTORY_TOPIC = "inventory_updates";

    /**
     * Constructor for ProducerService
     * @param kafkaProducerService The KafkaProducerService to be used for sending messages to Kafka topics
     */
    @Autowired
    public ProducerService(KafkaProducerService<String> kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    /**
     * Sends a message to the inventory_updates topic with the given key and message 
     * @param key The key for the message, which can be used for partitioning in Kafka
     * @param message The message to be sent, which should be a string representation of the inventory update
     * @throws Exception if there is an error while sending the message to Kafka
     */
    public void sendInventoryMessage(String key, String message) throws Exception {
        logger.info(String.format("Sending message to Kafka topic %s with key %s and value %s", INVENTORY_TOPIC, key, message));
        kafkaProducerService.sendMessage(INVENTORY_TOPIC, key, message);
    }
}
