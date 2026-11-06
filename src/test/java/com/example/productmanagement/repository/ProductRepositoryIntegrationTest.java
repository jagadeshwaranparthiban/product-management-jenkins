
package com.example.productmanagement.repository;

import com.example.productmanagement.TestcontainersConfiguration;
import com.example.productmanagement.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepo productRepository;

    // TEST 1: SAVE AND RETRIEVE PRODUCT
    @Test
    void shouldSaveAndRetrieveProduct() {
        Product product = new Product(
                "Keyboard",
                "Mechanical Keyboard",
                new BigDecimal("3499.99"),
                15
        );

        Product savedProduct = productRepository.saveAndFlush(product);
        assertThat(savedProduct.getId()).isNotNull();

        Optional<Product> retrievedProduct = productRepository.findById(savedProduct.getId());
        assertThat(retrievedProduct).isPresent();

        assertThat(retrievedProduct.get().getName())
                .isEqualTo("Keyboard");
        assertThat(retrievedProduct.get().getPrice())
                .isEqualByComparingTo("3499.99");
    }


    // TEST 2: RETRIEVE ALL PRODUCTS
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

        productRepository.save(p1);
        productRepository.save(p2);

        List<Product> products = productRepository.findAll();

        assertThat(products).hasSize(2);
        assertThat(products)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Keyboard", "Mouse");
    }


    // TEST 3: UPDATE PRODUCT
    @Test
    void shouldUpdateProduct() {
        Product product = new Product(
                "Keyboard",
                "Mechanical Keyboard",
                new BigDecimal("3499.99"),
                15
        );

        Product savedProduct = productRepository.saveAndFlush(product);

        savedProduct.setPrice(new BigDecimal("4999.99"));
        savedProduct.setQuantity(20);

        productRepository.saveAndFlush(savedProduct);

        Product updatedProduct =
                productRepository.findById(savedProduct.getId())
                        .orElseThrow();

        assertThat(updatedProduct.getPrice())
                .isEqualByComparingTo("4999.99");
        assertThat(updatedProduct.getQuantity())
                .isEqualTo(20);
    }


    // TEST 4: DELETE PRODUCT
    @Test
    void shouldDeleteProduct() {
        Product product = new Product(
                "Keyboard",
                "Mechanical Keyboard",
                new BigDecimal("3499.99"),
                15
        );

        Product savedProduct = productRepository.saveAndFlush(product);

        productRepository.deleteById(savedProduct.getId());
        productRepository.flush();

        assertThat(
                productRepository.existsById(savedProduct.getId())
        ).isFalse();
    }
}
