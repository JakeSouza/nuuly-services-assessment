package com.nuuly;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * This table tracks the item (SKU) being purchased and the number of times that item was purchased.
 */
@Entity
public class Favorites {
    @Id
    private String sku;
    private int count;

    /**
     * Default constructor for the Favorites class.
     */
    protected Favorites() {}

    /**
     * Constructor for the Favorites class. Initializes the SKU and count for this favorites item.
     * @param sku The SKU for this favorites item.
     * @param count The count for this favorites item.
     */
    public Favorites(String sku, int count) {
        this.sku = sku;
        this.count = count;
    }

    /**
     * Returns a string representation of the favorites item, including the SKU and count.
     * @return A string representation of the favorites item.
     */
    @Override
    public String toString() {
        return String.format(
                "Favorites[sku='%s', count=%d]",
                sku, count
        );
    }

    /**
     * Gets the count for this favorites item.
     * @return The count for this favorites item.
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the count for this favorites item.
     * @param i The new count for this favorites item.
     */
    public void setCount(int i) {
        this.count = i;
    }

    /**
     * Gets the SKU for this favorites item.
     * @return The SKU for this favorites item.
     */
    public String getSku() {
        return sku;
    }
}
