package com.nuuly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ControllerTest {

    @Mock
    private ProducerService producerService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private FavoritesRepository favoritesRepository;

    private Controller controller;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        controller = new Controller(producerService);
        setField(controller, "inventoryRepository", inventoryRepository);
        setField(controller, "favoritesRepository", favoritesRepository);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void updateInventory_createsNewRecord_whenSkuDoesNotExist() {
        when(inventoryRepository.findById("sku1")).thenReturn(Optional.empty());

        int result = controller.updateInventory("sku1", 10);

        assertEquals(10, result);
        verify(inventoryRepository).save(argThat(inv -> "sku1".equals(inv.getSku()) && inv.getCount() == 10));
    }

    @Test
    void updateInventory_updatesExistingRecord_whenSkuExists() {
        Inventory existing = new Inventory("sku1", 5);
        when(inventoryRepository.findById("sku1")).thenReturn(Optional.of(existing));

        int result = controller.updateInventory("sku1", 7);

        assertEquals(12, result);
        verify(inventoryRepository).save(argThat(inv -> "sku1".equals(inv.getSku()) && inv.getCount() == 12));
    }

    @Test
    void createPurchaseOrder_returnsCreated_whenUpdateInventorySucceeds() {
        Controller controllerSpy = spy(controller);
        doReturn(10).when(controllerSpy).updateInventory("sku1", 10);

        ResponseEntity<?> response = controllerSpy.createPurchaseOrder("sku1", 10);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Successfully added 10 of SKU sku1 to inventory for total of 10"));
    }

    @Test
    void createPurchaseOrder_returnsServerError_whenUpdateInventoryThrows() {
        Controller controllerSpy = spy(controller);
        doThrow(new RuntimeException("oops")).when(controllerSpy).updateInventory("sku1", 10);

        ResponseEntity<?> response = controllerSpy.createPurchaseOrder("sku1", 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Failed to create inventory for SKU sku1 with amount 10"));
    }

    @Test
    void purchase_returnsBadRequest_whenNullLists() {
        ResponseEntity<?> response = controller.purchase(null, List.of(1));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("must be the same length and not null"));
    }

    @Test
    void purchase_returnsBadRequest_whenSizeMismatch() {
        ResponseEntity<?> response = controller.purchase(List.of("a"), List.of(1, 2));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("must be the same length and not null"));
    }

    @Test
    void purchase_returnsOk_whenAllSuccessful() throws Exception {
        Inventory inv = new Inventory("a", 5);
        when(inventoryRepository.findById("a")).thenReturn(Optional.of(inv));
        doNothing().when(producerService).sendInventoryMessage(eq("a"), eq("1"));

        ResponseEntity<?> response = controller.purchase(List.of("a"), List.of(1));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Successfully purchased"));
        verify(inventoryRepository).save(argThat(i -> i.getSku().equals("a") && i.getCount() == 4));
        verify(producerService).sendInventoryMessage("a", "1");
    }

    @Test
    void purchase_continueProcessing_whenProducerThrows() throws Exception {
        Inventory inv = new Inventory("a", 5);
        when(inventoryRepository.findById("a")).thenReturn(Optional.of(inv));
        doThrow(new RuntimeException("kafka down")).when(producerService).sendInventoryMessage("a", "1");

        ResponseEntity<?> response = controller.purchase(List.of("a"), List.of(1));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Successfully purchased"));
        verify(inventoryRepository).save(argThat(i -> i.getSku().equals("a") && i.getCount() == 4));
    }

    @Test
    void purchase_returnsPartialFailure_whenMixedResults() throws Exception {
        Inventory inv = new Inventory("a", 5);
        when(inventoryRepository.findById("a")).thenReturn(Optional.of(inv));
        when(inventoryRepository.findById("b")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.purchase(List.of("a", "b"), List.of(1, 1));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Successfully purchased"));
        assertTrue(response.getBody().toString().contains("SKU: a, Purchased Count: 1"));
        assertTrue(response.getBody().toString().contains("not present in our system"));
        assertTrue(response.getBody().toString().contains("SKU: b"));
    }

    @Test
    void purchase_returnsFailure_whenAllFailed() {
        when(inventoryRepository.findById("a")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.purchase(List.of("a"), List.of(1));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Could not purchase any of the items"));
    }

    @Test
    void purchase_returnsFailure_whenInsufficientInventory() {
        Inventory inv = new Inventory("a", 2);
        when(inventoryRepository.findById("a")).thenReturn(Optional.of(inv));

        ResponseEntity<?> response = controller.purchase(List.of("a"), List.of(5));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Not enough inventory for the following SKUs"));
        assertTrue(response.getBody().toString().contains("SKU: a, Remaining Amount: 2"));
    }

    @Test
    void favorites_returnsBadRequest_forZeroCount() {
        ResponseEntity<?> response = controller.favorites("most", 0);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("count must be greater than 0"));
    }

    @Test
    void favorites_returnsOk_forMostAndCountLimiting() {
        Favorites a = new Favorites("sku1", 100);
        Favorites b = new Favorites("sku2", 20);
        when(favoritesRepository.count()).thenReturn(2L);
        when(favoritesRepository.findAllByOrderByCountDesc()).thenReturn(List.of(a, b));

        ResponseEntity<?> response = controller.favorites("most", 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("sku1"));
        assertFalse(response.getBody().toString().contains("sku2"));
    }

    @Test
    void favorites_returnsOk_forInvalidFilterDefaultingToBoth() {
        Favorites a = new Favorites("sku1", 100);
        Favorites b = new Favorites("sku2", 20);
        when(favoritesRepository.count()).thenReturn(2L);
        when(favoritesRepository.findAllByOrderByCountDesc()).thenReturn(List.of(a, b));
        when(favoritesRepository.findAllByOrderByCountAsc()).thenReturn(List.of(b, a));

        ResponseEntity<?> response = controller.favorites("invalid", 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Invalid filter defaults to returnging both, but due to the message building logic,
        // the response will contain least popular items (the last message built)
        assertTrue(response.getBody().toString().contains("Invalid filter"));
        assertTrue(response.getBody().toString().contains("least popular"));
        assertTrue(response.getBody().toString().contains("sku"));
    }

    @Test
    void favorites_returnsOk_forFilterBoth() {
        Favorites a = new Favorites("sku1", 100);
        Favorites b = new Favorites("sku2", 20);
        when(favoritesRepository.count()).thenReturn(2L);
        when(favoritesRepository.findAllByOrderByCountDesc()).thenReturn(List.of(a, b));
        when(favoritesRepository.findAllByOrderByCountAsc()).thenReturn(List.of(b, a));

        ResponseEntity<?> response = controller.favorites("both", 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Invalid filter defaults to returnging both, but due to the message building logic,
        // the response will contain least popular items (the last message built)
        assertTrue(response.getBody().toString().contains("least popular"));
        assertTrue(response.getBody().toString().contains("most popular"));
    }

    @Test
    void favorites_returnsOk_forLeastFilter() {
        Favorites a = new Favorites("sku1", 100);
        Favorites b = new Favorites("sku2", 20);
        when(favoritesRepository.count()).thenReturn(2L);
        when(favoritesRepository.findAllByOrderByCountAsc()).thenReturn(List.of(b, a));

        ResponseEntity<?> response = controller.favorites("least", 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("sku2"));
        assertFalse(response.getBody().toString().contains("sku1"));
    }
}
