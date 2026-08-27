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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Option {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
