package com.nuuly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        logger.info("Received create order for {} of SKU {}", receiptAmount, sku);

        try {
            // Update inventory 
            int finalInventory = updateInventory(sku, receiptAmount);
            String msg = String.format("Successfully added %d of SKU %s to inventory for total of %d)", receiptAmount, sku, finalInventory);
            logger.info(msg);
            // Return success message
            return new ResponseEntity<>(msg, CREATED);
        } catch (RuntimeException e) {
            // Log and handle error
            String errorMsg = String.format("Failed to create inventory for SKU %s with amount %d", sku, receiptAmount);
            logger.error(errorMsg, e);
            return new ResponseEntity<>(errorMsg, INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Helper method to update inventory when we receive a purchase order in table
     * 
     * @param sku: The stock keeping unit as alphanumeric digits assigned to a product
     * @param receiptAmount: The number of SKUs that are received by this purchase order
     * @return The total amount of inventory for the SKU after the update
     */
    protected int updateInventory(String sku, int receiptAmount) {
        // Try to fetch existing inventory for the SKU
        Optional<Inventory> existingInventory = inventoryRepository.findById(sku);
        
        // If existing we update the value
        if (existingInventory.isPresent()) {
            Inventory inventory = existingInventory.get();
            int updatedAmount = inventory.getCount() + receiptAmount;
            inventory.setCount(updatedAmount);
            inventoryRepository.save(inventory);
            return updatedAmount;
        }

        // Else create a new record
        Inventory newInventory = new Inventory(sku, receiptAmount);
        inventoryRepository.save(newInventory);
        return receiptAmount;
    }

    /**
     * When a garment is actually purchased by us, we want to decrement the inventory to represent that the item was
     * purchased.
     *
     * @param skus   List of SKUs to purchase
     * @param amounts List of quantities for each SKU
     * @return That the purchase was successful (or partially successful)
     */
    @PostMapping("/purchase")
    public ResponseEntity<?> purchase(
            @RequestParam("skus") List<String> skus,
            @RequestParam("amounts") List<Integer> amounts
    ) {
        logger.info("Received purchase order for {} of SKU {}", amounts, skus);

        // Ensure we have valid input lists
        if (skus == null || amounts == null || skus.size() != amounts.size()) {
            String errorMsg = "Skus and amounts must be the same length and not null";
            logger.warn(errorMsg);
            return new ResponseEntity<>(errorMsg, BAD_REQUEST);
        }

        // Call Kafka producer to async fill favorites table
        publishFavorites(skus, amounts);

        List<String> successSkus = new ArrayList<>();
        List<String> failedSkus = new ArrayList<>();

        // Process each purchase from the list
        for (int i = 0; i < skus.size(); i++) {
            processSinglePurchase(skus.get(i), amounts.get(i), successSkus, failedSkus);
        }
        
        // Send response to user based on success
        return makePurchaseResponse(successSkus, failedSkus);
    }

    /**
     * Publishes purchase messages to Kafka to update favorites table
     *
     * @param skus: List of SKUs being purchased
     * @param amounts: List of amounts being purchased for each SKU, must be the same
    */
    protected void publishFavorites(List<String> skus, List<Integer> amounts) {
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
    }

    /**
     * Processes individual purchases to update inventory numbers based on purchased items
     * and tracks successful and failed purchases for future response to user
     * 
     * @param sku: The stock keeping unit as alphanumeric digits assigned to a product
     * @param amount: The number of SKUs being purchased
     * @param successSkus: List to track successful purchases
     * @param failedSkus: List to track failed purchases
     */
    protected void processSinglePurchase(
            String sku,
            int amount,
            List<String> successSkus,
            List<String> failedSkus
    ) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findById(sku);
        
        // If we have inventory for this SKU already 
        if (inventoryOpt.isPresent()) {
            logger.info("We have inventory for this SKU, checking if we have enough to fulfill the order");
            Inventory currentInventory = inventoryOpt.get();
            
            // Check if  we have enough inventory to fufill order
            if (currentInventory.getCount() < amount) {
                // If we don't, log and track failed purchase
                logger.info(String.format("Not enough inventory for SKU %s. Current inventory: %d, requested amount: %d", sku, currentInventory.getCount(), amount));
                failedSkus.add(sku);
            } else {
                // If we do, decrement inventory and track successful purchase
                currentInventory.setCount(currentInventory.getCount() - amount);
                inventoryRepository.save(currentInventory);
                successSkus.add(sku);
                logger.info(String.format("Successfully purchased %d of SKU %s", amount, sku));
            }
        } else {
            // If we don't have any inventory for the SKU being purchased, return an error message
            logger.info(String.format("We don't have any records for SKU %s, please check the SKU and try again", sku));
            failedSkus.add(sku);
        }
    }

    /**
     * Helper method to create purchase response message based on which SKUs were successfully purchased and which were not
     * @param successSkus: List of SKUs that were successfully purchased
     * @param failedSkus: List of SKUs that were not successfully purchased
     * @return ResponseEntity with appropriate message and status code based on which SKUs were successfully purchased and which were not
     */
    protected ResponseEntity<String> makePurchaseResponse(List<String> successSkus, List<String> failedSkus) {
        // If all purchases were successful
        if (failedSkus.isEmpty()) {
            String returnMsg = String.format("Successfully purchased the following items: %s.", successSkus);
            return new ResponseEntity<>(returnMsg, OK);
        }
        // If all purchases were unsuccessful 
        else if (successSkus.isEmpty()) {
            String returnMsg = String.format("Could not purchase any of the items requested due to insufficient inventory or invalid SKU: %s.", failedSkus);
            return new ResponseEntity<>(returnMsg, BAD_REQUEST);
        } 
        // Mixed success and failure case
        else {
            String returnMsg = String.format("Successfully purchased the following items: %s. Could not purchase the following items due to insufficient inventory or invalid SKU: %s.", successSkus, failedSkus);
            return new ResponseEntity<>(returnMsg, BAD_REQUEST);
        }
    }

    /**
     * From a business perspective, we want to understand what our customers like and don't like. We want to get a list
     * of favorite items ranked by how many were purchased.
     *
     * @param filter most|least
     * @param count  number of items to return
     * @return A list of favorite items
     */
    @GetMapping("/favorites")
    public ResponseEntity<?> favorites(
        @RequestParam("filter") String filter,
        @RequestParam("count") int count
    ) {
        logger.info(String.format("Received request for the %s popular %d items", filter, count));
        List<Favorites> favorites = new ArrayList<>();
        // If the filter is "most", return the most popular items.
        if(filter.equals("most")) {
            favorites = favoritesRepository.findAllByOrderByCountDesc();
        }
        // If the filter is "least", return the least popular items. 
        else if(filter.equals("least")) {
            favorites = favoritesRepository.findAllByOrderByCountAsc();
        } 
        // If the filter is anything else, return an error message.
        else {
            return new ResponseEntity<>("To get the most popular items, use filter=most. To get the least popular items, use filter=least, please try again.", BAD_REQUEST);
        }
        logger.info(String.format("Getting the %s popular %d items:", filter, count));

        // if requesting more items than in favorites table, return all items in favorites table
        if(count > favorites.size()) {
            count = favorites.size();
        }

        // Return the requested number of items based on filter
        List<Favorites> requestFavorites = favorites.subList(0, count);
        return new ResponseEntity<>(requestFavorites.toString(), OK); 
    }
}
