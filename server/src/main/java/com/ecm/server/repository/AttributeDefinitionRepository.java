package com.ecm.server.repository;

import com.ecm.server.model.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, UUID> {

    Optional<AttributeDefinition> findByKey(String key);

    boolean existsByKey(String key);

    List<AttributeDefinition> findByStatusNot(String status);
}
