package com.ecm.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Employee profile keyed by the authentication account id. */
@Entity
@Table(name = "employees")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee implements Persistable<UUID> {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "avatar_file_id")
    private UUID avatarFileId;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Column(name = "salary", nullable = false)
    @Builder.Default
    private Long salary = 0L;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private LocalDate joinedAt = LocalDate.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void setAccount(Account account) {
        this.account = account;
        this.accountId = account == null ? null : account.getId();
    }

    @Override
    @Transient
    public boolean isNew() {
        return createdAt == null;
    }

    @Transient
    @Override
    public UUID getId() {
        return accountId;
    }
}
