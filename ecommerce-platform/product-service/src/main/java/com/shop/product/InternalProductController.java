package com.shop.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Service-to-service endpoints. Not routed by the gateway, so not reachable from outside. */
@RestController
@RequestMapping("/internal/products")
public class InternalProductController {

    private final ProductRepository repo;

    public InternalProductController(ProductRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<Void> reserve(@PathVariable Long id, @RequestParam int quantity) {
        return repo.reserve(id, quantity) == 1
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @PostMapping("/{id}/release")
    public void release(@PathVariable Long id, @RequestParam int quantity) {
        repo.release(id, quantity);
    }
}
