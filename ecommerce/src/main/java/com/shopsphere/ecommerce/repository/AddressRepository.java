package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdAndDeletedFalse(Long userId);

    Optional<Address> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);
}
