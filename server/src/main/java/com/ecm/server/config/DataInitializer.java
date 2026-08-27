package com.ecm.server.config;

import com.ecm.server.model.Account;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Role;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Initialize default system roles
        Role adminRole = getOrCreateRole("ROLE_ADMIN");
        getOrCreateRole("ROLE_EMPLOYEE");
        getOrCreateRole("ROLE_CUSTOMER");

        // 2. Initialize default admin user if not exists
        if (!accountRepository.existsByUsername("admin")) {
            Account adminAccount = Account.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(adminRole)
                    .status("ACTIVE")
                    .build();
            Account savedAccount = accountRepository.save(adminAccount);

            Employee adminEmployee = Employee.builder()
                    .account(savedAccount)
                    .fullName("System Administrator")
                    .email("admin@ecm.com")
                    .phone("0900000001")
                    .status("ACTIVE")
                    .build();
            employeeRepository.save(adminEmployee);

            log.info("Initialized default administrator account [admin / Admin@123]");
        }
    }

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).status("ACTIVE").build()));
    }
}
