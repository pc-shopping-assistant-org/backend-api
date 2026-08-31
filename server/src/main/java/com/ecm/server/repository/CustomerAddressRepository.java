package com.ecm.server.repository;

import com.ecm.server.model.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findByCustomerAccountIdOrderByIsDefaultDescCreatedAtAsc(UUID accountId);

    Optional<CustomerAddress> findByCustomerAccountIdAndIsDefaultTrue(UUID accountId);

    Optional<CustomerAddress> findByIdAndCustomerAccountId(UUID id, UUID accountId);

    @Query("SELECT COUNT(a) > 0 FROM CustomerAddress a WHERE a.customer.accountId = :accountId AND a.isDefault = true")
    boolean existsDefaultForCustomer(@Param("accountId") UUID accountId);
}
