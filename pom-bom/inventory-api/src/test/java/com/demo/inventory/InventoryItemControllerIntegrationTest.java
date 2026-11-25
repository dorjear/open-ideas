package com.demo.inventory;

import com.demo.testsupport.TestResultLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static com.demo.testsupport.TestResultLogger.step;
import static com.demo.testsupport.TestHttpLogging.perform;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Starts the full application context: MockMvc -> controller -> real JPA repository -> H2.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:inventory-controller-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ExtendWith(TestResultLogger.class)
class InventoryItemControllerIntegrationTest {

    private static final String VALID_ITEM = """
            {"sku":"SKU-001","name":"Widget","quantity":10,"price":9.99}
            """;
    private static final String INVALID_ITEM = """
            {"sku":" ","name":"","quantity":-1,"price":-0.01}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryItemRepository repository;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void crudRequestsPersistChangesInH2() throws Exception {
        step("createInventoryItem");
        var created = perform(mvc, post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ITEM))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andReturn();

        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String path = "/api/items/" + id;
        assertEquals("http://localhost" + path, created.getResponse().getHeader("Location"));
        InventoryItem saved = repository.findById(id).orElseThrow();
        assertEquals("SKU-001", saved.getSku());
        assertEquals("Widget", saved.getName());
        assertEquals(10, saved.getQuantity());
        assertEquals(new BigDecimal("9.99"), saved.getPrice());

        step("enquiryInventoryItem");
        perform(mvc, get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.sku").value("SKU-001"));
        step("listInventoryItem");
        perform(mvc, get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        step("updateInventoryItem");
        perform(mvc, put(path).contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"SKU-NEW","name":"New item","quantity":12,"price":19.95}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.sku").value("SKU-NEW"));

        InventoryItem updated = repository.findById(id).orElseThrow();
        assertEquals("SKU-NEW", updated.getSku());
        assertEquals("New item", updated.getName());
        assertEquals(12, updated.getQuantity());
        assertEquals(new BigDecimal("19.95"), updated.getPrice());
        assertEquals(1, repository.count());

        step("deleteInventoryItem");
        perform(mvc, delete(path))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        step("verifyInventoryItemDeleted");
        assertFalse(repository.existsById(id));
        perform(mvc, get(path)).andExpect(status().isNotFound());
        perform(mvc, get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void missingItemReturnsNotFound(String method) throws Exception {
        perform(mvc, request(HttpMethod.valueOf(method), "/api/items/999999")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ITEM))
                .andExpect(status().isNotFound());

        assertEquals(0, repository.count());
    }

    @Test
    void invalidCreateDoesNotPersistItem() throws Exception {
        perform(mvc, post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON).content(INVALID_ITEM))
                .andExpect(status().isBadRequest());

        assertEquals(0, repository.count());
    }

    @Test
    void invalidUpdateDoesNotChangePersistedItem() throws Exception {
        InventoryItem saved = repository.save(
                new InventoryItem("SKU-001", "Widget", 10, new BigDecimal("9.99")));

        perform(mvc, put("/api/items/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(INVALID_ITEM))
                .andExpect(status().isBadRequest());

        InventoryItem unchanged = repository.findById(saved.getId()).orElseThrow();
        assertEquals("SKU-001", unchanged.getSku());
        assertEquals("Widget", unchanged.getName());
        assertEquals(10, unchanged.getQuantity());
        assertEquals(new BigDecimal("9.99"), unchanged.getPrice());
        assertEquals(1, repository.count());
    }
}
