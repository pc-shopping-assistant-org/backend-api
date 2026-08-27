package com.ecm.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "attribute_definitions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeDefinition {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(name = "unit", length = 50)
    private String unit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_values", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> allowedValues = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aliases", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> aliases = new ArrayList<>();

    @Column(name = "filterable", nullable = false)
    @Builder.Default
    private boolean filterable = false;

    @Column(name = "comparable", nullable = false)
    @Builder.Default
    private boolean comparable = false;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
