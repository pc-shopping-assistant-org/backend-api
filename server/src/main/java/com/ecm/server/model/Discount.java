package com.ecm.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "discounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Discount {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "type", nullable = false, length = 10)
    private String type; // PERCENT, FIXED

    @Column(name = "value", nullable = false)
    private Integer value;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "scope", nullable = false, length = 50)
    private String scope; // ALL, PRODUCT, CATEGORY, ORDER

    @Column(name = "min_order_amount", nullable = false)
    @Builder.Default
    private Long minOrderAmount = 0L;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, EXPIRED, DISABLED, DELETED

    @OneToMany(mappedBy = "discount", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<DiscountProductVariant> discountProductVariants = new LinkedHashSet<>();
}
