package com.demo.inventory;

import com.demo.testsupport.TestResultLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.demo.testsupport.TestHttpLogging.controllerCall;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({TestResultLogger.class, MockitoExtension.class})
class InventoryItemControllerTest {

    @Mock
    private InventoryItemRepository repository;

    @InjectMocks
    private InventoryItemController controller;

    @Test
    void getAllReturnsItemsFromRepository() {
        List<InventoryItem> items = List.of(item("SKU-001"), item("SKU-002"));
        when(repository.findAll()).thenReturn(items);

        List<InventoryItem> result = controllerCall("GET", "/api/items", null, () -> controller.getAll());

        assertSame(items, result);
        verify(repository).findAll();
    }

    @Test
    void getByIdReturnsItemWhenFound() {
        InventoryItem item = item("SKU-001");
        when(repository.findById(7L)).thenReturn(Optional.of(item));

        var response = controllerCall("GET", "/api/items/7", null, () -> controller.getById(7L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(item, response.getBody());
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        var response = controllerCall("GET", "/api/items/7", null, () -> controller.getById(7L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void createSavesItemAndReturnsCreatedResponse() {
        InventoryItem item = item("SKU-001");
        InventoryItem saved = item("SKU-001");
        saved.setId(9L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/items");
        request.setServerName("localhost");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(repository.save(item)).thenReturn(saved);

        try {
            var response = controllerCall("POST", "/api/items", item, () -> controller.create(item));

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertSame(saved, response.getBody());
            assertTrue(response.getHeaders().getLocation().toString().endsWith("/api/items/9"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void updateChangesAndSavesItemWhenFound() {
        InventoryItem existing = item("SKU-OLD");
        InventoryItem updates = new InventoryItem("SKU-NEW", "New item", 12, new BigDecimal("19.95"));
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var response = controllerCall("PUT", "/api/items/7", updates, () -> controller.update(7L, updates));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(existing, response.getBody());
        assertEquals("SKU-NEW", existing.getSku());
        assertEquals("New item", existing.getName());
        assertEquals(12, existing.getQuantity());
        assertEquals(new BigDecimal("19.95"), existing.getPrice());
        verify(repository).save(existing);
    }

    @Test
    void updateReturnsNotFoundWhenItemIsMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        InventoryItem updates = item("SKU-NEW");
        var response = controllerCall("PUT", "/api/items/7", updates, () -> controller.update(7L, updates));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteRemovesItemAndReturnsNoContentWhenFound() {
        when(repository.existsById(7L)).thenReturn(true);

        var response = controllerCall("DELETE", "/api/items/7", null, () -> controller.delete(7L));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(repository).deleteById(7L);
    }

    @Test
    void deleteReturnsNotFoundWhenItemIsMissing() {
        when(repository.existsById(7L)).thenReturn(false);

        var response = controllerCall("DELETE", "/api/items/7", null, () -> controller.delete(7L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private InventoryItem item(String sku) {
        return new InventoryItem(sku, "Demo item", 5, new BigDecimal("9.99"));
    }
}
