package com.ecm.server.config;

import com.ecm.server.model.Account;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Role;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.PaymentMethodRepository;
import com.ecm.server.repository.RoleRepository;
import com.ecm.server.repository.ShippingMethodRepository;
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
    private final PaymentMethodRepository paymentMethodRepository;
    private final ShippingMethodRepository shippingMethodRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Initialize default system roles
        Role adminRole = getOrCreateRole("ROLE_ADMIN");
        getOrCreateRole("ROLE_EMPLOYEE");
        getOrCreateRole("ROLE_CUSTOMER");
        seedPaymentMethods();
        seedShippingMethods();

        // 2. Initialize default admin user if not exists
        if (!accountRepository.existsByEmailIgnoreCase("admin@ecm.com")) {
            Account adminAccount = Account.builder()
                    .email("admin@ecm.com")
                    .phone("0900000001")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .role(adminRole)
                    .status("ACTIVE")
                    .build();
            Account savedAccount = accountRepository.save(adminAccount);

            Employee adminEmployee = Employee.builder()
                    .firstName("System")
                    .lastName("Administrator")
                    .gender("MALE")
                    .joinedAt(java.time.LocalDate.now())
                    .build();
            adminEmployee.setAccount(savedAccount);
            employeeRepository.save(adminEmployee);

            log.info("Initialized default administrator account [admin@ecm.com / Admin@123]");
        }
    }

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).status("ACTIVE").build()));
    }

    private void seedPaymentMethods() {
        createPaymentMethodIfMissing("COD", "Cash on delivery");
        createPaymentMethodIfMissing("STRIPE_CARD", "Stripe card");
        createPaymentMethodIfMissing("BANK_TRANSFER", "Bank transfer");
    }

    private void createPaymentMethodIfMissing(String code, String name) {
        paymentMethodRepository.findByCodeIgnoreCase(code).orElseGet(() ->
                paymentMethodRepository.save(com.ecm.server.model.PaymentMethod.builder()
                        .code(code).name(name).status("ACTIVE").build()));
    }

    private void seedShippingMethods() {
        createShippingMethodIfMissing("STANDARD", "Standard delivery", 0L);
        createShippingMethodIfMissing("EXPRESS", "Express delivery", 30_000L);
        createShippingMethodIfMissing("SAME_DAY", "Same-day delivery", 50_000L);
    }

    private void createShippingMethodIfMissing(String code, String name, long fee) {
        shippingMethodRepository.findByCodeIgnoreCase(code).orElseGet(() ->
                shippingMethodRepository.save(com.ecm.server.model.ShippingMethod.builder()
                        .code(code).name(name).fee(fee).status("ACTIVE").build()));
    }
}
