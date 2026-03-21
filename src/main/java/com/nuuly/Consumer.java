package com.nuuly;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * This class will consume the messages that are produced on the given topic.
 * It will then perform some action on that message.
 */
@Component
public class Consumer {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String INVENTORY_TOPIC = "inventory_updates";

    @Autowired
    private FavoritesRepository favoritesRepository;

    /**
     * This method will consume messages from the inventory_updates Kafka topic, then parse and process it within the favorites table.
     *
     * @param content the message consumed from the inventory_updates topic
     */
    @KafkaListener(topics = INVENTORY_TOPIC)
    public void processMessage(ConsumerRecord<String, String> content) {
        logger.info(String.format("Received message for SKU %s with amount %s", content.key(), content.value()));
        String sku = content.key();
        int amount = parsePurchasedAmount(content);
        if (amount < 0) {
            logger.error("Invalid amount parsed from Kafka message, skipping processing for this message");
            // Functionality to send to DLQ and create support ticket to investigate bad message format would go here
            return;
        }
        int totalAmount = 0;

        // Check if the SKU being purchased is already in our favorites
        Optional<Favorites> favorite = favoritesRepository.findById(sku);

        // If the SKU being purchased is already in our favorites, increment the count by the amount being purchased.
        if (favorite.isPresent()) {
            logger.info("Item already in favorites, incrementing count");
            Favorites currentFavorite = favorite.get();
            currentFavorite.setCount(currentFavorite.getCount() + amount);
            storeFavorite(currentFavorite);
        }
        else {
            logger.info("Newly purchased item, adding to favorites");
            totalAmount = amount;
            Favorites currentFavorite = new Favorites(sku, amount);
            storeFavorite(currentFavorite);
        }

        // Log the purchase for our own records
        logger.info(String.format("Successfully added %s of SKU %s to our favorites for a total of %d", amount, sku, totalAmount));
    }

    /**
     * Helper method to parse the amount being purchased from the Kafka message content
     * 
     * @param content Kafka ConsumerRecord containing the message with the amount being purchased as the value
     * @return the amount being purchased as an integer, or -1 if there is an error parsing the amount
     */
    protected int parsePurchasedAmount(ConsumerRecord<String, String> content) {
        try {
            int amount = objectMapper.readValue(content.value(), Integer.class);
            return amount;
        } catch (Exception e) {
            logger.error("Failed to parse purchased amount from Kafka message", e);
            return -1;
        }
    }

    /**
     * Helper method to store the updated favorite in the favorites table
     *
     * @param favorite: The Favorites object to be stored in the favorites table
     */
    protected void storeFavorite(Favorites favorite) {
        try {
            favoritesRepository.save(favorite);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to add SKU %s to favorites with count %d", favorite.toString(), favorite.getCount());
            logger.error(errorMsg);
            // Functionality to retry with back off and then send to DLQ would go here
        }
    }
}
