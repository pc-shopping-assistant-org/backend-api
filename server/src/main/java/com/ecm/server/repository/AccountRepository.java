package com.ecm.server.repository;

import com.ecm.server.model.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    @EntityGraph(attributePaths = {"role"})
    Optional<Account> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"role"})
    Optional<Account> findByPhone(String phone);

    @EntityGraph(attributePaths = {"role"})
    @Query("SELECT a FROM Account a WHERE LOWER(a.email) = LOWER(:identifier) OR a.phone = :identifier")
    Optional<Account> findByLoginIdentifier(@Param("identifier") String identifier);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    /**
     * Account status is the source of truth for authorization decisions. A
     * scalar projection avoids loading the role/profile graph in the JWT
     * filter while still invalidating tokens after an account is locked.
     */
    @Query("SELECT a.status FROM Account a WHERE a.id = :accountId")
    Optional<String> findStatusById(@Param("accountId") UUID accountId);

}
