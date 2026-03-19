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

    @KafkaListener(topics = INVENTORY_TOPIC)
    public void processMessage(ConsumerRecord<String, String> content) {
        logger.info("Received message: " + content.toString());
        logger.info(String.format("Received message for SKU %s with amount %s", content.key(), content.value()));
        String sku = content.key();
        int amount;
        try {
            amount = objectMapper.readValue(content.value(), Integer.class);
        } catch (Exception e) {
            logger.error("Failed to parse purchased amount from Kafka message", e);
            return; // skip processing invalid message
        }
        int totalAmount = 0;

        // Check if the SKU being purchased is already in our favorites
        Optional<Favorites> favorite = favoritesRepository.findById(sku);

        // If the SKU being purchased is already in our favorites, increment the count by the amount being purchased.
        if (favorite.isPresent()) {
            logger.info("Item already in favorites, incrementing count");
            Favorites currentFavorite = favorite.get();
            currentFavorite.setCount(currentFavorite.getCount() + amount);
            favoritesRepository.save(currentFavorite);
        }
        else {
            logger.info("Newly purchased item, adding to favorites");
            totalAmount = amount;
            favoritesRepository.save(new Favorites(sku, amount));
        }

        // Log the purchase for our own records
        logger.info(String.format("Successfully added %s of SKU %s to our favorites for a total of %d", amount, sku, totalAmount));
    }

}
