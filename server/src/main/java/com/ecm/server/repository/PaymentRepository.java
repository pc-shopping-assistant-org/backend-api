package com.ecm.server.repository;

import com.ecm.server.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    List<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Optional<Payment> findByTransactionCode(String transactionCode);
}
