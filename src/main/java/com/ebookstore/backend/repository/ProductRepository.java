package com.ebookstore.backend.repository;

import com.ebookstore.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByNameContainingIgnoreCase(String search);
    List<Product> findByCategoryIdAndIdNot(Long categoryId, Long excludeId);
}