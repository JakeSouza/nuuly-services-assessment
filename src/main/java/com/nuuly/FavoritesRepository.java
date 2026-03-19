package com.nuuly;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoritesRepository extends CrudRepository<Favorites, String> {
    List<Favorites> findAllByOrderByCountDesc();
    List<Favorites> findAllByOrderByCountAsc();
}
