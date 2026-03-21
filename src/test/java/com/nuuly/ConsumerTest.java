package com.nuuly;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ConsumerTest {

    @Mock
    private FavoritesRepository favoritesRepository;

    @InjectMocks
    private Consumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void parsePurchasedAmount_returnsNegativeOne_whenMalformedJson() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("inventory_updates", 0, 0L, "sku1", "not-a-number");

        int amount = consumer.parsePurchasedAmount(record);

        assertEquals(-1, amount);
    }

    @Test
    void parsePurchasedAmount_returnsValue_whenValidJson() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("inventory_updates", 0, 0L, "sku1", "5");

        int amount = consumer.parsePurchasedAmount(record);

        assertEquals(5, amount);
    }

    @Test
    void processMessage_skipsSave_whenAmountInvalid() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("inventory_updates", 0, 0L, "sku1", "x");

        consumer.processMessage(record);

        verify(favoritesRepository, never()).save(any(Favorites.class));
    }

    @Test
    void processMessage_updatesExistingFavorite_whenPresent() {
        Favorites existing = new Favorites("sku1", 5);
        when(favoritesRepository.findById("sku1")).thenReturn(Optional.of(existing));

        ConsumerRecord<String, String> record = new ConsumerRecord<>("inventory_updates", 0, 0L, "sku1", "3");

        consumer.processMessage(record);

        verify(favoritesRepository).save(argThat(f -> f.getCount() == 8));
    }

    @Test
    void processMessage_createsFavorite_whenNotPresent() {
        when(favoritesRepository.findById("sku1")).thenReturn(Optional.empty());

        ConsumerRecord<String, String> record = new ConsumerRecord<>("inventory_updates", 0, 0L, "sku1", "4");

        consumer.processMessage(record);

        verify(favoritesRepository).save(argThat(f -> f.getCount() == 4));
    }
}
