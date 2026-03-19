package com.nuuly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException.BadRequest;

import static org.springframework.http.HttpStatus.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This is our base API server to run HTTP requests against. Currently, these are set up to just return HTTP responses
 * and not actually do anything. Your goal is to make changes here using the rest of the example code to complete the
 * objectives.
 */
@RestController
public class Controller {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private final ProducerService producer;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private FavoritesRepository favoritesRepository;

    @Autowired
    public Controller(ProducerService producer) {
        this.producer = producer;
    }

    /**
     * When garments are ready to be sold, a purchase order is issued to a seller that a certain
     * agreed upon quantity of a product is wanted. These products get allocated into our inventory.
     *
     * @param sku: The stock keeping unit as alphanumeric digits assigned to a product
     * @param receiptAmount: The number of SKUs that are received by this purchase order
     * @return That the purchase order was created
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPurchaseOrder(
            @RequestParam("sku") String sku,
            @RequestParam("receiptAmount") int receiptAmount
    ) {
        logger.info(String.format("Received create order for %d of SKU %s", receiptAmount, sku));
        String returnMsg;
        int totalAmount = 0;

        // Check if we already have inventory for the SKU being added
        Optional<Inventory> inventory = inventoryRepository.findById(sku);
        
        // If we do, increment the inventory by the amount being added. If we don't, create a new inventory record for that SKU with the amount being added.
        if (inventory.isPresent()) {
            logger.info("We already have inventory for this SKU, adding to the current inventory");
            Inventory currentInventory = inventory.get();
            totalAmount = currentInventory.getCount() + receiptAmount;
            currentInventory.setCount(totalAmount);
            inventoryRepository.save(currentInventory);
        }
        else {
            logger.info("New SKU, adding to inventory");
            totalAmount = receiptAmount;
            inventoryRepository.save(new Inventory(sku, receiptAmount));
        }

        // Log the purchase order for our own records and return a success message
        returnMsg = String.format("Successfully added %s of SKU %s to our inventory for a total of %d", receiptAmount, sku, totalAmount);
        logger.info(returnMsg);
        return new ResponseEntity<>(returnMsg, CREATED);
    }

    /**
     * When a garment is actually purchased by us, we want to decrement the inventory to represent that the item was
     * purchased.
     *
     * @param sku: The item that was purchased
     * @param amount: How many of that item that was purchased
     * @return That the purchase was successful
     */
    @PostMapping("/purchase")
    public ResponseEntity<?> purchase(
            @RequestParam("skus") List<String> skus,
            @RequestParam("amounts") List<Integer> amounts
    ) {
        // Avoid String.format conversion bugs (amounts is a List<String> when formatted).
        logger.info("Received purchase order for {} of SKU {}", amounts, skus);
        // Call Kafka producer to async fill favorites table
        try {
            logger.info("Sending purchase messages to Kafka to update favorites");
            for (int i = 0; i < skus.size(); i++) {
                producer.sendInventoryMessage(skus.get(i), Integer.toString(amounts.get(i)));
            }
        } catch (Exception e) {
            // Log and handle failure
            logger.error("Failed to send Kafka message", e);
            // Functionality to retry with back off and then send to DLQ would go here
            // No failure here, need to process purchase regardless of favorites functionality
        }

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        for (int i = 0; i < skus.size(); i++) {
            String sku = skus.get(i);
            int amount = amounts.get(i);
            // Check if we have inventory for the SKU being purchased
            Optional<Inventory> inventory = inventoryRepository.findById(sku);

            // If we have inventory for the SKU being purchased, check if we have enough inventory to fulfill the order. If we do, decrement the inventory by the amount purchased and return a success message. If we don't, return an error message.
            if (inventory.isPresent()) {
                logger.info("We have inventory for this SKU, checking if we have enough to fulfill the order");
                Inventory currentInventory = inventory.get();

                // If we don't have enough inventory to fulfill the order, return an error message
                if(currentInventory.getCount() < amount) {
                    logger.info(String.format("Not enough inventory for SKU %s. Current inventory: %d, requested amount: %d", sku, currentInventory.getCount(), amount));
                    errorMessages.add(sku);
                    continue; // skip to the next item being purchased
                }

                // We have enough inventory to fulfill the order, decrement the inventory by the amount purchased and return a success message
                currentInventory.setCount(currentInventory.getCount() - amount);
                inventoryRepository.save(currentInventory);
                successMessages.add(sku);
                logger.info(String.format("Successfully purchased %d of SKU %s", amount, sku));
                
            }
            else{
                // If we don't have any inventory for the SKU being purchased, return an error message
                logger.info(String.format("We don't have any records for SKU %s, please check the SKU and try again", sku));
                errorMessages.add(sku);
            }
        }

        if (errorMessages.isEmpty()) {
            String returnMsg = String.format("Successfully purchased the following items: %s.", successMessages.toString());
            return new ResponseEntity<>(returnMsg, OK);
        } 
        else if(successMessages.isEmpty()) {
            String returnMsg = String.format("Could not purchase any of the items requested due to insufficient inventory or invalid SKU: %s.", errorMessages.toString());
            return new ResponseEntity<>(returnMsg, BAD_REQUEST);
        }
        else {
            String returnMsg = String.format("Successfully purchased the following items: %s. Could not purchased following iteems due to insufficient inventory or invalid SKU: %s.", successMessages.toString(), errorMessages.toString());
            return new ResponseEntity<>(returnMsg, BAD_REQUEST);
        }
        
    }

    /**
     * From a business perspective, we want to understand what our customers like and don't like. We want to get a list
     * of favorite items ranked by how many were purchased.
     *
     * @return A list of favorite items
     */
    @GetMapping("/favorites")
    public ResponseEntity<?> favorites(
        @RequestParam("filter") String filter,
        @RequestParam("count") int count
    ) {
        logger.info(String.format("Received request for the %s popular %d items", filter, count));
        // If the filter is "most", return the most popular items. If the filter is "least", return the least popular items. If the filter is anything else, return an error message.
        if(filter.equals("most")) {
            List<Favorites> favorites = favoritesRepository.findAllByOrderByCountDesc();
            logger.info("Here are the most popular items:");
            logger.info(favorites.toString());

            if(count > favorites.size()) {
                count = favorites.size();
            }

            List<Favorites> requestFavorites = favorites.subList(0, count);
            return new ResponseEntity<>(requestFavorites.toString(), OK);
        }
        else if(filter.equals("least")) {
            List<Favorites> favorites = favoritesRepository.findAllByOrderByCountAsc();
            logger.info("Here are the least popular items:");
            logger.info(favorites.toString());

            if(count > favorites.size()) {
                count = favorites.size();
            }

            List<Favorites> requestFavorites = favorites.subList(0, count);
            return new ResponseEntity<>(requestFavorites.toString(), OK);
        }       
        return new ResponseEntity<>("To get the most popular items, use filter=most. To get the least popular items, use filter=least, please try again.", BAD_REQUEST);
    }
}
