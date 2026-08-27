package com.ecm.server.repository;

import com.ecm.server.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {

    boolean existsByName(String name);

    List<Brand> findByStatusNot(String status);
}
