package com.demo.inventory;

import com.demo.testsupport.TestResultLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.demo.testsupport.TestHttpLogging.perform;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Starts Spring MVC and a real controller, with the repository replaced by a Spring-managed mock.
@WebMvcTest(InventoryItemController.class)
@ExtendWith(TestResultLogger.class)
class InventoryItemControllerMvcTest {

    private static final String VALID_ITEM = """
            {"sku":"SKU-001","name":"Widget","quantity":10,"price":9.99}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InventoryItemRepository repository;

    @Test
    void getAllReturnsItemsAsJson() throws Exception {
        when(repository.findAll()).thenReturn(List.of(item(7L)));

        perform(mvc, get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].sku").value("SKU-001"));
    }

    @Test
    void getByIdReturnsItemWhenFound() throws Exception {
        when(repository.findById(7L)).thenReturn(Optional.of(item(7L)));

        perform(mvc, get("/api/items/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.price").value(9.99));
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() throws Exception {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        perform(mvc, get("/api/items/7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDeserializesAndSavesItemAndReturnsLocation() throws Exception {
        when(repository.save(any(InventoryItem.class))).thenReturn(item(9L));

        perform(mvc, post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ITEM))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/items/9"))
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.sku").value("SKU-001"));

        verify(repository).save(argThat(item -> item.getId() == null
                && "SKU-001".equals(item.getSku())
                && "Widget".equals(item.getName())
                && Integer.valueOf(10).equals(item.getQuantity())
                && new BigDecimal("9.99").equals(item.getPrice())));
    }

    @Test
    void updateChangesAndSavesItemWhenFound() throws Exception {
        InventoryItem existing = new InventoryItem("SKU-OLD", "Old item", 5, new BigDecimal("1.00"));
        existing.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        perform(mvc, put("/api/items/7")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ITEM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.price").value(9.99));

        verify(repository).save(existing);
    }

    @Test
    void updateReturnsNotFoundWhenMissing() throws Exception {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        perform(mvc, put("/api/items/7")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ITEM))
                .andExpect(status().isNotFound());

        verify(repository, never()).save(any(InventoryItem.class));
    }

    @Test
    void deleteRemovesItemWhenFound() throws Exception {
        when(repository.existsById(7L)).thenReturn(true);

        perform(mvc, delete("/api/items/7"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(repository).deleteById(7L);
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() throws Exception {
        when(repository.existsById(7L)).thenReturn(false);

        perform(mvc, delete("/api/items/7"))
                .andExpect(status().isNotFound());

        verify(repository, never()).deleteById(7L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void invalidItemReturnsBadRequestWithoutCallingRepository(String method) throws Exception {
        String path = method.equals("POST") ? "/api/items" : "/api/items/7";

        perform(mvc, request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":" ","name":"","quantity":-1,"price":-0.01}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(repository);
    }

    private InventoryItem item(Long id) {
        InventoryItem item = new InventoryItem("SKU-001", "Widget", 10, new BigDecimal("9.99"));
        item.setId(id);
        return item;
    }
}
