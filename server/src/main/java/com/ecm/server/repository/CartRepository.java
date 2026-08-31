package com.ecm.server.repository;

import com.ecm.server.model.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.product"})
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.customer.accountId = :accountId")
    Optional<Cart> findActiveByAccountId(@Param("accountId") UUID accountId);

    /**
     * Serialize cart mutations and checkout for one account.  Without the
     * row lock two concurrent checkouts could both read the same ACTIVE cart
     * before either transaction marks it CONVERTED.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.product"})
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.customer.accountId = :accountId")
    Optional<Cart> findActiveByAccountIdForUpdate(@Param("accountId") UUID accountId);

    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.product"})
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.sessionToken = :sessionToken")
    Optional<Cart> findActiveBySessionToken(@Param("sessionToken") String sessionToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.product"})
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.sessionToken = :sessionToken")
    Optional<Cart> findActiveBySessionTokenForUpdate(@Param("sessionToken") String sessionToken);
}
