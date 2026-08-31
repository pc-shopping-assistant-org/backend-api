package com.ecm.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "discounts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Discount {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "code", unique = true, length = 50)
    private String code;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "discount_type", nullable = false, length = 10)
    private String discountType; // PERCENT, FIXED

    @Column(name = "value", nullable = false)
    private Integer value;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "application_scope", nullable = false, length = 20)
    private String applicationScope; // ORDER, ALL_ITEMS, CATEGORY, VARIANT

    @Column(name = "min_order_amount", nullable = false)
    @Builder.Default
    private Long minOrderAmount = 0L;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
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
    private Set<DiscountCategory> discountCategories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "discount", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<DiscountVariant> discountVariants = new LinkedHashSet<>();

}
