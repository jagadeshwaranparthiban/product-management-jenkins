
package com.example.productmanagement.controller;

import com.example.productmanagement.TestcontainersConfiguration;
import com.example.productmanagement.model.Product;
import com.example.productmanagement.repository.ProductRepo;

import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.containsInAnyOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepo productRepository;


    // TEST 1: CREATE PRODUCT

    @Test
    void shouldCreateProduct() throws Exception {

        String requestBody = """
                {
                    "name": "Keyboard",
                    "description": "Mechanical Keyboard",
                    "price": 3499.99,
                    "quantity": 15
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))

                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.description")
                        .value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.price").value(3499.99))
                .andExpect(jsonPath("$.quantity").value(15))
                .andExpect(header().exists("Location"));
    }


    // TEST 2: GET ALL PRODUCTS

    @Test
    void shouldGetAllProducts() throws Exception {

        productRepository.saveAndFlush(
                new Product(
                        "Keyboard",
                        "Mechanical Keyboard",
                        new BigDecimal("3499.99"),
                        15
                )
        );

        productRepository.saveAndFlush(
                new Product(
                        "Mouse",
                        "Wireless Mouse",
                        new BigDecimal("999.99"),
                        25
                )
        );

        mockMvc.perform(get("/api/products")
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name",
                        containsInAnyOrder("Keyboard", "Mouse")));
    }


    // TEST 3: GET PRODUCT BY ID

    @Test
    void shouldGetProductById() throws Exception {

        Product product = new Product(
                "Keyboard",
                "Mechanical Keyboard",
                new BigDecimal("3499.99"),
                15
        );

        Product savedProduct =
                productRepository.saveAndFlush(product);

        mockMvc.perform(get("/api/products/{id}",
                        savedProduct.getId()))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(savedProduct.getId().toString()))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.price").value(3499.99));
    }


    // TEST 4: PRODUCT NOT FOUND

    @Test
    void shouldReturn404WhenProductDoesNotExist() throws Exception {

        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/products/{id}", randomId))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Product not found with ID: " + randomId));
    }


    // TEST 5: UPDATE PRODUCT

    @Test
    void shouldUpdateProduct() throws Exception {

        Product product = new Product(
                "Keyboard",
                "Old Keyboard",
                new BigDecimal("2000.00"),
                10
        );

        Product savedProduct =
                productRepository.saveAndFlush(product);

        String requestBody = """
                {
                    "name": "Keyboard Pro",
                    "description": "Updated Mechanical Keyboard",
                    "price": 4999.99,
                    "quantity": 20
                }
                """;

        mockMvc.perform(put("/api/products/{id}",
                        savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keyboard Pro"))
                .andExpect(jsonPath("$.description")
                        .value("Updated Mechanical Keyboard"))
                .andExpect(jsonPath("$.price").value(4999.99))
                .andExpect(jsonPath("$.quantity").value(20));

        Product updatedProduct = productRepository
                .findById(savedProduct.getId())
                .orElseThrow();

        assertThat(updatedProduct.getName())
                .isEqualTo("Keyboard Pro");

        assertThat(updatedProduct.getPrice())
                .isEqualByComparingTo("4999.99");
    }


    // TEST 6: DELETE PRODUCT

    @Test
    void shouldDeleteProduct() throws Exception {

        Product product = new Product(
                "Keyboard",
                "Mechanical Keyboard",
                new BigDecimal("3499.99"),
                15
        );

        Product savedProduct =
                productRepository.saveAndFlush(product);

        mockMvc.perform(delete("/api/products/{id}",
                        savedProduct.getId()))

                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(productRepository
                .findById(savedProduct.getId()))
                .isEmpty();
    }


    // TEST 7: VALIDATION FAILURE

    @Test
    void shouldReturn400ForInvalidProductRequest() throws Exception {

        String requestBody = """
                {
                    "name": "",
                    "description": "Invalid product",
                    "price": -100,
                    "quantity": -5
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name")
                        .value("Product name is required"))
                .andExpect(jsonPath("$.validationErrors.price")
                        .value("Price must be greater than zero"))
                .andExpect(jsonPath("$.validationErrors.quantity")
                        .value("Quantity cannot be negative"));
    }
}
