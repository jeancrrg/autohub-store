package com.autohubstore.userservice.infrastructure.persistence.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AddressJpaRepository extends JpaRepository<AddressJpaEntity, UUID> {

    List<AddressJpaEntity> findAllByUserId(UUID userId);

    @Modifying
    @Query("UPDATE AddressJpaEntity a SET a.isDefault = false WHERE a.userId = :userId")
    void clearDefaultByUserId(@Param("userId") UUID userId);

}
