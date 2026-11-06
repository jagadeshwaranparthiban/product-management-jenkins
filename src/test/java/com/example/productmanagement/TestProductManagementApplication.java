package com.example.productmanagement;

import org.springframework.boot.SpringApplication;

public class TestProductManagementApplication {

	public static void main(String[] args) {
		SpringApplication.from(ProductManagementApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
