package com.ecm.server.mapper;

import com.ecm.server.model.File;
import com.ecm.server.model.Option;
import com.ecm.server.model.ProductImage;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.model.VariantOption;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductVariantMapperTest {

    private final ProductVariantMapper mapper = new ProductVariantMapperImpl();

    @Test
    void publicVariantResponseExposesOnlyActiveImagesAndOptions() {
        Option activeOption = Option.builder()
                .type("COLOR")
                .name("Black")
                .value("black")
                .status("ACTIVE")
                .build();
        Option inactiveOption = Option.builder()
                .type("COLOR")
                .name("White")
                .value("white")
                .status("INACTIVE")
                .build();

        ProductVariant variant = ProductVariant.builder()
                .sku("SKU-1")
                .listPrice(100L)
                .quantity(2)
                .warrantyMonths(12)
                .variantOptions(new LinkedHashSet<>(List.of(
                        VariantOption.builder().option(activeOption).status("ACTIVE").build(),
                        VariantOption.builder().option(inactiveOption).status("ACTIVE").build(),
                        VariantOption.builder().option(activeOption).status("DELETED").build())))
                .images(new LinkedHashSet<>(List.of(
                        ProductImage.builder()
                                .name("active")
                                .status("ACTIVE")
                                .file(File.builder().publicUrl("https://cdn.example/active.jpg").build())
                                .build(),
                        ProductImage.builder()
                                .name("inactive")
                                .status("INACTIVE")
                                .file(File.builder().publicUrl("https://cdn.example/inactive.jpg").build())
                                .build(),
                        ProductImage.builder()
                                .name("deleted")
                                .status("DELETED")
                                .file(File.builder().publicUrl("https://cdn.example/deleted.jpg").build())
                                .build())))
                .build();

        var response = mapper.toResponse(variant);

        assertEquals(1, response.getImages().size());
        assertEquals("active", response.getImages().getFirst().getName());
        assertEquals("https://cdn.example/active.jpg", response.getImageUrl());
        assertEquals(1, response.getOptions().size());
        assertEquals("Black", response.getOptions().getFirst().getName());
        assertTrue(response.getImages().stream().allMatch(image -> "ACTIVE".equals(image.getStatus())));
        assertFalse(response.getOptions().stream().anyMatch(option -> "White".equals(option.getName())));
    }

    @Test
    void warrantyParserAcceptsPositiveMonthValues() {
        assertEquals(12, mapper.parseWarrantyMonths("12 months"));
        assertEquals(24, mapper.parseWarrantyMonths("24 tháng"));
        assertEquals(6, mapper.parseWarrantyMonths("6"));
    }

    @Test
    void warrantyParserRejectsNonPositiveOrMalformedValues() {
        assertThrows(IllegalArgumentException.class, () -> mapper.parseWarrantyMonths("0"));
        assertThrows(IllegalArgumentException.class, () -> mapper.parseWarrantyMonths("abc"));
        assertThrows(IllegalArgumentException.class, () -> mapper.parseWarrantyMonths("1.5"));
    }
}
