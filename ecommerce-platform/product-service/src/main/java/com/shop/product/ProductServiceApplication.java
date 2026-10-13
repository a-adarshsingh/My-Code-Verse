package com.shop.product;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner seedProducts(ProductRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new Product("Mechanical Keyboard", "Hot-swappable, 75% layout", new BigDecimal("89.99"), 50, "electronics"));
                repo.save(new Product("Noise-Cancelling Headphones", "Over-ear, 30h battery", new BigDecimal("199.00"), 20, "electronics"));
                repo.save(new Product("Java Concurrency in Practice", "Paperback", new BigDecimal("42.50"), 100, "books"));
            }
        };
    }
}
