package com.ecm.server.repository;

import com.ecm.server.model.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    @EntityGraph(attributePaths = {"role"})
    Optional<Account> findByUsername(String username);

    boolean existsByUsername(String username);
}
