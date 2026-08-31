package com.ecm.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "brands")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Brand {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "seo_name", nullable = false, unique = true, length = 255)
    private String seoName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_file_id")
    private UUID imageFileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_file_id", insertable = false, updatable = false)
    private File imageFile;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
