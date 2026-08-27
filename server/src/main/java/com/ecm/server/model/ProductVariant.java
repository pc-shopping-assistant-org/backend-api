package com.ecm.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "product_variants")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "price_sale", nullable = false)
    private Integer priceSale;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "inventory_policy", nullable = false, length = 15)
    @Builder.Default
    private String inventoryPolicy = "DENY";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specifications", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> specifications = new HashMap<>();

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "warranty", length = 100)
    private String warranty;

    @Column(name = "barcode", unique = true, length = 100)
    private String barcode;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "release_at")
    private LocalDate releaseAt;

    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    @OneToMany(mappedBy = "productVariant", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ProductImage> images = new LinkedHashSet<>();

    @OneToMany(mappedBy = "productVariant", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<VariantOption> variantOptions = new LinkedHashSet<>();
}
