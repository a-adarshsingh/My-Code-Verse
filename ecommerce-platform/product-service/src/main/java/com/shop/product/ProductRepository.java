package com.shop.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryIgnoreCase(String category);

    /** Atomic conditional decrement: no overselling, even with many concurrent orders. Returns 1 on success. */
    @Transactional
    @Modifying
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
    int reserve(@Param("id") Long id, @Param("qty") int qty);

    @Transactional
    @Modifying
    @Query("update Product p set p.stock = p.stock + :qty where p.id = :id")
    int release(@Param("id") Long id, @Param("qty") int qty);
}
