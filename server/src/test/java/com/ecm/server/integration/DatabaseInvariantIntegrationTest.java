package com.ecm.server.integration;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.PaymentFilterRequest;
import com.ecm.server.dto.response.PaymentDetailResponse;
import com.ecm.server.service.AdminPaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the cross-row invariants that cannot be expressed by JPA mocks or
 * a simple column CHECK constraint.  The test intentionally uses PostgreSQL
 * through the application's real Flyway schema.
 */
@SpringBootTest
class DatabaseInvariantIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AdminPaymentService adminPaymentService;

    private UUID roleId;
    private UUID customerAccountId;
    private UUID employeeAccountId;
    private UUID customerAddressOneId;
    private UUID customerAddressTwoId;
    private UUID categoryId;
    private UUID productId;
    private UUID variantId;
    private UUID fileOneId;
    private UUID fileTwoId;
    private UUID optionOneId;
    private UUID optionTwoId;
    private UUID shippingMethodId;
    private UUID paymentMethodId;
    private UUID orderId;
    private UUID customerCartId;
    private UUID sessionCartId;
    private UUID discountId;

    @BeforeEach
    void seedFixtures() {
        roleId = UUID.randomUUID();
        customerAccountId = UUID.randomUUID();
        employeeAccountId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        fileOneId = UUID.randomUUID();
        fileTwoId = UUID.randomUUID();
        optionOneId = UUID.randomUUID();
        optionTwoId = UUID.randomUUID();
        shippingMethodId = UUID.randomUUID();
        paymentMethodId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        customerCartId = UUID.randomUUID();
        sessionCartId = UUID.randomUUID();
        discountId = UUID.randomUUID();

        jdbc.update("INSERT INTO roles (id, name, status) VALUES (?, ?, 'ACTIVE')",
                roleId, "TEST_ROLE_" + roleId);
        insertAccount(customerAccountId, "customer-" + customerAccountId + "@test.invalid", "098" + suffix(customerAccountId));
        insertAccount(employeeAccountId, "employee-" + employeeAccountId + "@test.invalid", "097" + suffix(employeeAccountId));
        jdbc.update("""
                        INSERT INTO customers (account_id, first_name, last_name)
                        VALUES (?, 'Test', 'Customer')
                        """, customerAccountId);
        jdbc.update("""
                        INSERT INTO employees (account_id, first_name, last_name, gender, salary)
                        VALUES (?, 'Test', 'Employee', 'MALE', 0)
                        """, employeeAccountId);

        jdbc.update("""
                        INSERT INTO categories (id, name, seo_name, status)
                        VALUES (?, ?, ?, 'ACTIVE')
                        """, categoryId, "Test Category " + categoryId, "test-category-" + categoryId);
        jdbc.update("""
                        INSERT INTO products (id, name, seo_name, category_id, status)
                        VALUES (?, ?, ?, ?, 'ACTIVE')
                        """, productId, "Test Product " + productId, "test-product-" + productId, categoryId);
        jdbc.update("""
                        INSERT INTO product_variants
                            (id, product_id, list_price, quantity, sku, warranty_months, status, created_by)
                        VALUES (?, ?, 1000, 10, ?, 12, 'ACTIVE', ?)
                        """, variantId, productId, "TEST-SKU-" + variantId, employeeAccountId);

        fileOneId = insertFile("one-" + fileOneId);
        fileTwoId = insertFile("two-" + fileTwoId);
        jdbc.update("""
                        INSERT INTO options (id, type, name, value, status)
                        VALUES (?, 'COLOR', ?, 'Black', 'ACTIVE'),
                               (?, ' color ', ?, 'White', 'ACTIVE')
                        """, optionOneId, "test-option-one-" + optionOneId,
                optionTwoId, "test-option-two-" + optionTwoId);
        jdbc.update("""
                        INSERT INTO shipping_methods (id, code, name, status)
                        VALUES (?, ?, 'Test shipping', 'ACTIVE')
                        """, shippingMethodId, "TEST_SHIPPING_" + shippingMethodId);
        jdbc.update("""
                        INSERT INTO payment_methods (id, code, name, status)
                        VALUES (?, ?, 'Test payment', 'ACTIVE')
                        """, paymentMethodId, "TEST_PAYMENT_" + paymentMethodId);
    }

    @AfterEach
    void removeFixtures() {
        jdbc.update("DELETE FROM discount_categories WHERE discount_id = ?", discountId);
        jdbc.update("DELETE FROM discount_variants WHERE discount_id = ?", discountId);
        jdbc.update("DELETE FROM discounts WHERE id = ?", discountId);
        jdbc.update("DELETE FROM payments WHERE order_id = ?", orderId);
        jdbc.update("DELETE FROM orders WHERE id = ?", orderId);
        jdbc.update("DELETE FROM cart_items WHERE cart_id IN (?, ?)", customerCartId, sessionCartId);
        jdbc.update("DELETE FROM carts WHERE id IN (?, ?)", customerCartId, sessionCartId);
        jdbc.update("DELETE FROM customer_addresses WHERE customer_id = ?", customerAccountId);
        jdbc.update("DELETE FROM variant_options WHERE product_variant_id = ?", variantId);
        jdbc.update("DELETE FROM product_images WHERE product_variant_id = ?", variantId);
        jdbc.update("DELETE FROM options WHERE id IN (?, ?)", optionOneId, optionTwoId);
        jdbc.update("DELETE FROM files WHERE id IN (?, ?)", fileOneId, fileTwoId);
        jdbc.update("DELETE FROM product_variants WHERE id = ?", variantId);
        jdbc.update("DELETE FROM products WHERE id = ?", productId);
        jdbc.update("DELETE FROM categories WHERE id = ?", categoryId);
        jdbc.update("DELETE FROM customers WHERE account_id = ?", customerAccountId);
        jdbc.update("DELETE FROM employees WHERE account_id = ?", employeeAccountId);
        jdbc.update("DELETE FROM accounts WHERE id IN (?, ?)", customerAccountId, employeeAccountId);
        jdbc.update("DELETE FROM payment_methods WHERE id = ?", paymentMethodId);
        jdbc.update("DELETE FROM shipping_methods WHERE id = ?", shippingMethodId);
        jdbc.update("DELETE FROM roles WHERE id = ?", roleId);
    }

    @Test
    void allowsOnlyOneDefaultAddressPerCustomer() {
        customerAddressOneId = UUID.randomUUID();
        customerAddressTwoId = UUID.randomUUID();
        insertAddress(customerAddressOneId, "One");

        assertThrows(DataAccessException.class,
                () -> insertAddress(customerAddressTwoId, "Two"));
    }

    @Test
    void allowsOnlyOneActiveCartPerCustomerOrSession() {
        jdbc.update("""
                        INSERT INTO carts (id, customer_id, status)
                        VALUES (?, ?, 'ACTIVE')
                        """, customerCartId, customerAccountId);
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                        INSERT INTO carts (id, customer_id, status)
                        VALUES (?, ?, 'ACTIVE')
                        """, UUID.randomUUID(), customerAccountId));

        jdbc.update("""
                        INSERT INTO carts (id, session_token, status)
                        VALUES (?, ?, 'ACTIVE')
                        """, sessionCartId, "session-" + sessionCartId);
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                        INSERT INTO carts (id, session_token, status)
                        VALUES (?, ?, 'ACTIVE')
                        """, UUID.randomUUID(), "session-" + sessionCartId));
    }

    @Test
    void allowsOnlyOneActiveMainImagePerVariant() {
        jdbc.update("""
                        INSERT INTO product_images (id, product_variant_id, file_id, is_main, status)
                        VALUES (?, ?, ?, true, 'ACTIVE')
                        """, UUID.randomUUID(), variantId, fileOneId);

        assertThrows(DataAccessException.class, () -> jdbc.update("""
                        INSERT INTO product_images (id, product_variant_id, file_id, is_main, status)
                        VALUES (?, ?, ?, true, 'ACTIVE')
                        """, UUID.randomUUID(), variantId, fileTwoId));
    }

    @Test
    void allowsOnlyOnePaidPaymentPerOrder() {
        insertOrder();
        jdbc.update("""
                        INSERT INTO payments (id, order_id, payment_method_id, amount, paid_at, status)
                        VALUES (?, ?, ?, 1000, current_timestamp, 'PAID')
                        """, UUID.randomUUID(), orderId, paymentMethodId);

        assertThrows(DataAccessException.class, () -> jdbc.update("""
                        INSERT INTO payments (id, order_id, payment_method_id, amount, paid_at, status)
                        VALUES (?, ?, ?, 1000, current_timestamp, 'PAID')
                        """, UUID.randomUUID(), orderId, paymentMethodId));
    }

    @Test
    void rejectsTwoOptionsOfTheSameTypeOnOneVariant() {
        jdbc.update("""
                        INSERT INTO variant_options (id, product_variant_id, option_id, status)
                        VALUES (?, ?, ?, 'ACTIVE')
                        """, UUID.randomUUID(), variantId, optionOneId);

        assertThrows(DataAccessException.class, () -> jdbc.update("""
                        INSERT INTO variant_options (id, product_variant_id, option_id, status)
                        VALUES (?, ?, ?, 'ACTIVE')
                        """, UUID.randomUUID(), variantId, optionTwoId));
    }

    @Test
    void rejectsDiscountTargetsThatContradictScope() {
        jdbc.update("""
                        INSERT INTO discounts
                            (id, title, discount_type, value, application_scope, start_at, end_at, status)
                        VALUES (?, 'Test order discount', 'FIXED', 1, 'ORDER', ?, ?, 'ACTIVE')
                        """, discountId, Timestamp.from(Instant.now().minusSeconds(60)),
                Timestamp.from(Instant.now().plusSeconds(60)));

        assertThrows(DataAccessException.class, () -> jdbc.update("""
                        INSERT INTO discount_categories (discount_id, category_id)
                        VALUES (?, ?)
                """, discountId, categoryId));
    }

    @Test
    void paymentSearchMatchesCustomerNameKeyword() {
        insertOrder();
        jdbc.update("""
                        INSERT INTO payments (id, order_id, payment_method_id, amount, status)
                        VALUES (?, ?, ?, 1000, 'PENDING')
                        """, UUID.randomUUID(), orderId, paymentMethodId);

        CursorPageResponse<PaymentDetailResponse> result = adminPaymentService.getAdminPayments(
                PaymentFilterRequest.builder()
                        .keyword("  test customer  ")
                        .limit(20)
                        .build());

        assertEquals(1, result.getItems().size());
        assertEquals(orderId, result.getItems().get(0).getOrderId());
    }

    private void insertAccount(UUID id, String email, String phone) {
        jdbc.update("""
                        INSERT INTO accounts (id, email, phone, password_hash, role_id, status)
                        VALUES (?, ?, ?, 'test-hash', ?, 'ACTIVE')
                        """, id, email, phone, roleId);
    }

    private UUID insertFile(String key) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                        INSERT INTO files
                            (id, storage_provider, storage_key, original_name, mime_type, size_bytes, status)
                        VALUES (?, 'TEST', ?, ?, 'image/png', 1, 'ACTIVE')
                        """, id, key, key + ".png");
        return id;
    }

    private void insertAddress(UUID id, String name) {
        jdbc.update("""
                        INSERT INTO customer_addresses
                            (id, customer_id, recipient_name, phone, address_line, is_default)
                        VALUES (?, ?, ?, '0900000000', ?, true)
                        """, id, customerAccountId, name, name + " address");
    }

    private void insertOrder() {
        jdbc.update("""
                        INSERT INTO orders
                            (id, customer_id, shipping_method_id, subtotal_amount, discount_amount,
                             shipping_fee, total_amount, delivery_address, recipient_name, recipient_phone)
                        VALUES (?, ?, ?, 1000, 0, 0, 1000, 'Test address', 'Test recipient', '0900000000')
                        """, orderId, customerAccountId, shippingMethodId);
    }

    private static String suffix(UUID id) {
        return id.toString().replace("-", "").substring(0, 7);
    }
}
