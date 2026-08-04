package com.autohubstore.catalogservice.repository;

import com.autohubstore.catalogservice.domain.entity.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    Optional<Product> findBySlug(String slug);

}
