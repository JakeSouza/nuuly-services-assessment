package com.nuuly;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * This is the repository interface for managing favorites data.
 */
@Repository
public interface FavoritesRepository extends CrudRepository<Favorites, String> {
    /**
     * Finds all favorites ordered by count in descending order.
     * @return A list of favorites ordered by count in descending order.
     */
    List<Favorites> findAllByOrderByCountDesc();
    
    /**
     * Finds all favorites ordered by count in ascending order.
     * @return A list of favorites ordered by count in ascending order.
     */
    List<Favorites> findAllByOrderByCountAsc();
}
