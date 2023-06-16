package com.example.batch.repository;

import com.example.batch.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = "select max(product_id) from products", nativeQuery = true)
    long getCount();
}
