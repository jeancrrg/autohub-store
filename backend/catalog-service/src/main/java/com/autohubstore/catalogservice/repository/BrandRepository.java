package com.autohubstore.catalogservice.repository;

import com.autohubstore.catalogservice.domain.entity.Brand;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    List<Brand> findAllByOrderByNameAsc();

}
