package com.nuuly;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * This table tracks the item (SKU) currently in inventory and the number of items in inventory.
 */
@Entity
public class Inventory {

    @Id
    private String sku;
    private int count;

    /**
     * Default constructor for the Inventory class.
     */
    protected Inventory() {}

    /**
     * Constructor for the Inventory class. Initializes the SKU and count for this inventory item.
     * @param sku The SKU for this inventory item.
     * @param count The count for this inventory item.
     */
    public Inventory(String sku, int count) {
        this.sku = sku;
        this.count = count;
    }

    /**
     * Returns a string representation of the inventory item, including the SKU and count.
     * @return A string representation of the inventory item.
     */
    @Override
    public String toString() {
        return String.format(
                "Inventory[sku='%s', amount=%d]",
                sku, count
        );
    }
    
    /**
     * Gets the count for this inventory item.
     * @return The count for this inventory item.
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the count for this inventory item.
     * @param i The new count for this inventory item.
     */
    public void setCount(int i) {
        this.count = i;
    }

    /**
     * Gets the SKU for this inventory item.
     * @return The SKU for this inventory item.
     */
    public String getSku() {
        return sku;
    }
}

