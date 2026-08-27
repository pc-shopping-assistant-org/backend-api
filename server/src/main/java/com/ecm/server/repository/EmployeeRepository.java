package com.ecm.server.repository;

import com.ecm.server.model.Employee;
import org.springframework.data.domain.Pageable;
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
                WHERE (:keyword IS NULL OR LOWER(e.fullName) LIKE :keyword OR LOWER(e.email) LIKE :keyword OR LOWER(e.phone) LIKE :keyword OR LOWER(a.username) LIKE :keyword)
                  AND (:roleName IS NULL OR r.name = :roleName)
                  AND (:status IS NULL OR e.status = :status)
                ORDER BY e.id DESC
            """)
    List<Employee> findEmployeesInitial(
            @Param("keyword") String keyword,
            @Param("roleName") String roleName,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
                SELECT e FROM Employee e
                JOIN FETCH e.account a
                LEFT JOIN FETCH a.role r
                WHERE e.id < :cursor
                  AND (:keyword IS NULL OR LOWER(e.fullName) LIKE :keyword OR LOWER(e.email) LIKE :keyword OR LOWER(e.phone) LIKE :keyword OR LOWER(a.username) LIKE :keyword)
                  AND (:roleName IS NULL OR r.name = :roleName)
                  AND (:status IS NULL OR e.status = :status)
                ORDER BY e.id DESC
            """)
    List<Employee> findEmployeesAfterCursor(
            @Param("cursor") UUID cursor,
            @Param("keyword") String keyword,
            @Param("roleName") String roleName,
            @Param("status") String status,
            Pageable pageable
    );
}
