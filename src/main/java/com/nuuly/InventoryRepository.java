package com.nuuly;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * This is the repository interface for managing inventory data.
 */
@Repository
public interface InventoryRepository extends CrudRepository<Inventory, String> {}

