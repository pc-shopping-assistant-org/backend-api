package com.ecm.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "category_attributes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttribute {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_group_id", nullable = false)
    private CategoryAttributeGroup categoryGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private AttributeDefinition attribute;

    @Column(name = "required", nullable = false)
    @Builder.Default
    private boolean required = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
