package com.ecm.server.mapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMappingSupportTest {

    @Test
    void normalizesGenderForDatabaseCheckConstraints() {
        assertEquals("FEMALE", UserMappingSupport.normalizeCustomerGender(" female "));
        assertEquals("MALE", UserMappingSupport.normalizeEmployeeGender("male"));
        assertEquals("OTHER", UserMappingSupport.normalizeCustomerGender("Other"));
        assertNull(UserMappingSupport.normalizeCustomerGender(null));
        assertNull(UserMappingSupport.normalizeEmployeeGender("  "));
    }
}
