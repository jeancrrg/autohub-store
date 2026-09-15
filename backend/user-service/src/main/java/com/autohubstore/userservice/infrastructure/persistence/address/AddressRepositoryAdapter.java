package com.autohubstore.userservice.infrastructure.persistence.address;

import com.autohubstore.userservice.domain.model.Address;
import com.autohubstore.userservice.domain.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AddressRepositoryAdapter implements AddressRepository {

    private final AddressJpaRepository addressJpaRepository;
    private final AddressPersistenceMapper addressPersistenceMapper;

    @Override
    public Address save(Address address) {
        AddressJpaEntity entity = addressJpaRepository.save(addressPersistenceMapper.toJpaEntity(address));
        return addressPersistenceMapper.toDomain(entity);
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return addressJpaRepository.findById(id).map(addressPersistenceMapper::toDomain);
    }

    @Override
    public List<Address> findAllByUserId(UUID userId) {
        return addressJpaRepository.findAllByUserId(userId).stream()
                .map(addressPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void clearDefaultByUserId(UUID userId) {
        addressJpaRepository.clearDefaultByUserId(userId);
    }

    @Override
    public void delete(Address address) {
        addressJpaRepository.delete(addressPersistenceMapper.toJpaEntity(address));
    }

}
