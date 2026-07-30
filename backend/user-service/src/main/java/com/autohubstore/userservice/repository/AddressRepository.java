package com.autohubstore.userservice.repository;

import com.autohubstore.userservice.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findAllByUserId(UUID userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId")
    void clearDefaultByUserId(@Param("userId") UUID userId);
}
