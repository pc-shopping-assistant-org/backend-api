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

    @Query("SELECT e FROM Employee e JOIN FETCH e.account a WHERE LOWER(a.email) = LOWER(:email)")
    Optional<Employee> findByEmail(@Param("email") String email);

    @Query("SELECT e FROM Employee e JOIN FETCH e.account a WHERE a.phone = :phone")
    Optional<Employee> findByPhone(@Param("phone") String phone);

    @Query("SELECT COUNT(e) > 0 FROM Employee e JOIN e.account a WHERE LOWER(a.email) = LOWER(:email)")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT COUNT(e) > 0 FROM Employee e JOIN e.account a WHERE a.phone = :phone")
    boolean existsByPhone(@Param("phone") String phone);

    @Query("""
                SELECT e FROM Employee e
                JOIN FETCH e.account a
                LEFT JOIN FETCH a.role r
                WHERE (:keyword IS NULL OR LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE :keyword OR LOWER(a.email) LIKE :keyword OR LOWER(a.phone) LIKE :keyword)
                  AND (:roleName IS NULL OR r.name = :roleName)
                  AND (:status IS NULL OR a.status = :status)
                ORDER BY e.accountId DESC
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
                WHERE e.accountId < :cursor
                  AND (:keyword IS NULL OR LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE :keyword OR LOWER(a.email) LIKE :keyword OR LOWER(a.phone) LIKE :keyword)
                  AND (:roleName IS NULL OR r.name = :roleName)
                  AND (:status IS NULL OR a.status = :status)
                ORDER BY e.accountId DESC
            """)
    List<Employee> findEmployeesAfterCursor(
            @Param("cursor") UUID cursor,
            @Param("keyword") String keyword,
            @Param("roleName") String roleName,
            @Param("status") String status,
            Pageable pageable
    );
}
