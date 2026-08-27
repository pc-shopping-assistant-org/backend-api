package com.ecm.server.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Standard business status and error codes.
 */
@Getter
@RequiredArgsConstructor
public enum StatusCode {

    // =====================================================================
    // 1. Success Codes (200xx)
    // =====================================================================
    SUCCESS(20000, "Success", HttpStatus.OK),
    CREATED(20100, "Resource created successfully", HttpStatus.CREATED),
    UPDATED(20001, "Resource updated successfully", HttpStatus.OK),
    DELETED(20002, "Resource deleted successfully", HttpStatus.OK),

    // =====================================================================
    // 2. Generic Client Error Codes (400xx)
    // =====================================================================
    BAD_REQUEST(40000, "Invalid request parameters", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(40001, "Input validation failed", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_PARAMETER(40002, "Missing required request parameter", HttpStatus.BAD_REQUEST),
    MALFORMED_JSON(40003, "Malformed JSON request body", HttpStatus.BAD_REQUEST),

    // =====================================================================
    // 3. Authentication & Authorization Codes (401xx, 403xx)
    // =====================================================================
    UNAUTHORIZED(40100, "Unauthorized access, please login", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(40101, "Invalid username or password", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(40102, "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(40103, "Invalid authentication token", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "Access denied, you do not have permission", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED(40301, "Account is locked", HttpStatus.FORBIDDEN),
    ACCOUNT_INACTIVE(40302, "Account is inactive or deleted", HttpStatus.FORBIDDEN),

    // =====================================================================
    // 4. Resource Not Found Codes (404xx)
    // =====================================================================
    NOT_FOUND(40400, "Requested resource not found", HttpStatus.NOT_FOUND),
    ENDPOINT_NOT_FOUND(40401, "API endpoint not found", HttpStatus.NOT_FOUND),
    ACCOUNT_NOT_FOUND(40402, "Account not found", HttpStatus.NOT_FOUND),
    EMPLOYEE_NOT_FOUND(40403, "Employee profile not found", HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_FOUND(40404, "Customer profile not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(40405, "Category not found", HttpStatus.NOT_FOUND),
    BRAND_NOT_FOUND(40406, "Brand not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(40407, "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_VARIANT_NOT_FOUND(40408, "Product variant not found", HttpStatus.NOT_FOUND),
    ATTRIBUTE_NOT_FOUND(40409, "Attribute definition not found", HttpStatus.NOT_FOUND),
    DISCOUNT_NOT_FOUND(40410, "Discount coupon not found", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(40411, "Order not found", HttpStatus.NOT_FOUND),
    OPTION_NOT_FOUND(40412, "Option not found", HttpStatus.NOT_FOUND),
    CATEGORY_GROUP_NOT_FOUND(40413, "Category attribute group not found", HttpStatus.NOT_FOUND),
    CATEGORY_ATTRIBUTE_NOT_FOUND(40414, "Category attribute assignment not found", HttpStatus.NOT_FOUND),
    IMAGE_NOT_FOUND(40415, "Product image not found", HttpStatus.NOT_FOUND),
    SUPPLIER_NOT_FOUND(40416, "Supplier not found", HttpStatus.NOT_FOUND),

    // =====================================================================
    // 5. Conflict & Business Rule Codes (409xx, 422xx)
    // =====================================================================
    CONFLICT(40900, "Resource conflict occurred", HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS(40901, "Username already exists", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(40902, "Email already in use", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS(40903, "Phone number already in use", HttpStatus.CONFLICT),
    SKU_ALREADY_EXISTS(40904, "Product variant SKU already exists", HttpStatus.CONFLICT),
    DISCOUNT_CODE_ALREADY_EXISTS(40905, "Discount coupon code already exists", HttpStatus.CONFLICT),

    INSUFFICIENT_STOCK(42201, "Insufficient product stock quantity", HttpStatus.UNPROCESSABLE_CONTENT),
    DISCOUNT_EXPIRED(42202, "Discount code has expired or is not yet active", HttpStatus.UNPROCESSABLE_CONTENT),
    DISCOUNT_LIMIT_REACHED(42203, "Discount usage limit reached", HttpStatus.UNPROCESSABLE_CONTENT),
    INVALID_ORDER_STATE_TRANSITION(42204, "Invalid order status transition", HttpStatus.UNPROCESSABLE_CONTENT),
    PAYMENT_FAILED(42205, "Payment transaction failed", HttpStatus.UNPROCESSABLE_CONTENT),

    // =====================================================================
    // 6. Server & Third-Party Codes (500xx)
    // =====================================================================
    INTERNAL_SERVER_ERROR(50000, "An internal server error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR(50001, "Database access or persistence failure", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR(50200, "External service integration failed", HttpStatus.BAD_GATEWAY);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
