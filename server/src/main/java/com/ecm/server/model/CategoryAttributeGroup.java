package com.ecm.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "category_attribute_groups")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributeGroup {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "categoryGroup", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<CategoryAttribute> categoryAttributes = new ArrayList<>();
}
