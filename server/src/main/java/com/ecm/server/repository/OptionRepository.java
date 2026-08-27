package com.ecm.server.repository;

import com.ecm.server.model.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OptionRepository extends JpaRepository<Option, UUID> {

    List<Option> findByTypeIgnoreCaseAndStatusNot(String type, String status);

    List<Option> findByStatusNot(String status);

    boolean existsByName(String name);
}
