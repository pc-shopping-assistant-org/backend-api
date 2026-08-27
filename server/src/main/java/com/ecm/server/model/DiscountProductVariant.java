package com.ecm.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "discount_product_variants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountProductVariant {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, DELETED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
