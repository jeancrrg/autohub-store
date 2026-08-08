package com.autohubstore.catalogservice.repository;

import com.autohubstore.catalogservice.domain.entity.Category;

import com.autohubstore.catalogservice.domain.projection.CategoryProductCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsBySlug(String slug);

    List<Category> findAllByOrderByCreatedAt();

    @Query("""
        SELECT c.id AS categoryId,
               COUNT(p) AS productCount
          FROM Category c LEFT JOIN Product p ON p.category = c
         GROUP BY c.id
    """)
    List<CategoryProductCountProjection> findProductCounts();

}
