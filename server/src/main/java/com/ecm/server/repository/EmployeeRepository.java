package com.ecm.server.repository;

import com.ecm.server.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByAccountId(UUID accountId);

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("""
        SELECT e FROM Employee e
        JOIN FETCH e.account a
        LEFT JOIN FETCH a.role r
        WHERE (:cursor IS NULL OR e.id < :cursor)
          AND (:keyword IS NULL OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:roleName IS NULL OR r.name = :roleName)
          AND (:status IS NULL OR e.status = :status)
        ORDER BY e.id DESC
        LIMIT :queryLimit
    """)
    List<Employee> findEmployeesByCursor(
            @Param("cursor") UUID cursor,
            @Param("keyword") String keyword,
            @Param("roleName") String roleName,
            @Param("status") String status,
            @Param("queryLimit") int queryLimit
    );
}
