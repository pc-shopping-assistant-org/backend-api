package com.ecm.server.repository;

import com.ecm.server.model.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, UUID> {
    Optional<ShippingMethod> findByCodeIgnoreCaseAndStatus(String code, String status);
    Optional<ShippingMethod> findByCodeIgnoreCase(String code);
}
