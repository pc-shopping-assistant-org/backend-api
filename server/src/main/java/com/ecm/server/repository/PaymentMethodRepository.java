package com.ecm.server.repository;

import com.ecm.server.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    Optional<PaymentMethod> findByCodeIgnoreCaseAndStatus(String code, String status);
    Optional<PaymentMethod> findByCodeIgnoreCase(String code);
    List<PaymentMethod> findByStatusOrderByCreatedAtAsc(String status);
}
