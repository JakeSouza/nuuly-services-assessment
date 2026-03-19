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

    protected Favorites() {}

    public Favorites(String sku, int count) {
        this.sku = sku;
        this.count = count;
    }

    @Override
    public String toString() {
        return String.format(
                "Favorites[sku='%s', count=%d]",
                sku, count
        );
    }

    public int getCount() {
        return count;
    }

    public void setCount(int i) {
        this.count = i;
    }
}
