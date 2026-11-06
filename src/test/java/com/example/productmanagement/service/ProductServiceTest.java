
package com.example.productmanagement.service;

import com.example.productmanagement.dto.ProductRequest;
import com.example.productmanagement.dto.ProductResponse;
import com.example.productmanagement.model.Product;
import com.example.productmanagement.exception.ProductNotFoundException;
import com.example.productmanagement.repository.ProductRepo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepo productRepository;

    @InjectMocks
    private ProductService productService;


    // TEST 1: CREATE PRODUCT

    @Test
    void shouldCreateProduct() {

        ProductRequest request = new ProductRequest(
                "Keyboard",
                "Mechanical Keyboard",
                new BigDecimal("3499.99"),
                15
        );

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response =
                productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Keyboard");
        assertThat(response.price())
                .isEqualByComparingTo("3499.99");
        assertThat(response.quantity()).isEqualTo(15);

        verify(productRepository, times(1))
                .save(any(Product.class));
    }


    // TEST 2: GET ALL PRODUCTS

    @Test
    void shouldReturnAllProducts() {

        Product p1 = new Product(
                "Keyboard",
                "Mechanical Keyboard",
                new BigDecimal("3499.99"),
                15
        );

        Product p2 = new Product(
                "Mouse",
                "Wireless Mouse",
                new BigDecimal("999.99"),
                25
        );

        when(productRepository.findAll())
                .thenReturn(List.of(p1, p2));

        List<ProductResponse> products =
                productService.getAllProducts();

        assertThat(products).hasSize(2);
        assertThat(products)
                .extracting(ProductResponse::name)
                .containsExactly("Keyboard", "Mouse");

        verify(productRepository, times(1)).findAll();
    }


    // TEST 3: GET PRODUCT BY ID

    @Test
    void shouldReturnProductById() {

        UUID id = UUID.randomUUID();

        Product product = new Product(
                "Keyboard",
                "Mechanical Keyboard",
                new BigDecimal("3499.99"),
                15
        );

        when(productRepository.findById(id))
                .thenReturn(Optional.of(product));

        ProductResponse response =
                productService.getProductById(id);

        assertThat(response.name()).isEqualTo("Keyboard");
        assertThat(response.quantity()).isEqualTo(15);

        verify(productRepository).findById(id);
    }


    // TEST 4: PRODUCT NOT FOUND

    @Test
    void shouldThrowExceptionWhenProductNotFound() {

        UUID id = UUID.randomUUID();

        when(productRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.getProductById(id))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository).findById(id);
    }


    // TEST 5: UPDATE PRODUCT

    @Test
    void shouldUpdateProduct() {

        UUID id = UUID.randomUUID();

        Product existingProduct = new Product(
                "Keyboard",
                "Old Keyboard",
                new BigDecimal("2000.00"),
                10
        );

        ProductRequest request = new ProductRequest(
                "Keyboard Pro",
                "Updated Mechanical Keyboard",
                new BigDecimal("4999.99"),
                20
        );

        when(productRepository.findById(id))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response =
                productService.updateProduct(id, request);

        assertThat(response.name()).isEqualTo("Keyboard Pro");
        assertThat(response.description())
                .isEqualTo("Updated Mechanical Keyboard");
        assertThat(response.price())
                .isEqualByComparingTo("4999.99");
        assertThat(response.quantity()).isEqualTo(20);

        verify(productRepository).findById(id);
        verify(productRepository).save(existingProduct);
    }


    // TEST 6: DELETE PRODUCT

    @Test
    void shouldDeleteProduct() {

        UUID id = UUID.randomUUID();

        Product product = new Product(
                "Keyboard",
                "Mechanical Keyboard",
                new BigDecimal("3499.99"),
                15
        );

        when(productRepository.findById(id))
                .thenReturn(Optional.of(product));

        productService.deleteProduct(id);

        verify(productRepository, times(1))
                .delete(product);
    }
}
