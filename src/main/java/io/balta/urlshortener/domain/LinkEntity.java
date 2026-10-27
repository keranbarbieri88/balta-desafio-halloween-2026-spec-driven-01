package io.balta.urlshortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "links")
public class LinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 20, unique = true)
    private String code;

    @Column(name = "target_url", nullable = false, length = 2048)
    private String targetUrl;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "expires_at_utc")
    private Instant expiresAtUtc;

    @Column(name = "access_count", nullable = false)
    private Long accessCount;

    protected LinkEntity() {
        // Construtor padrão protegido exigido pelo JPA/Hibernate
    }

    public LinkEntity(String code, String targetUrl, Instant createdAtUtc, Instant expiresAtUtc) {
        this.code = Objects.requireNonNull(code, "O código não pode ser nulo");
        this.targetUrl = Objects.requireNonNull(targetUrl, "A URL de destino não pode ser nula");
        this.createdAtUtc = createdAtUtc != null ? createdAtUtc : Instant.now();
        this.expiresAtUtc = expiresAtUtc;
        this.accessCount = 0L;
    }

    public boolean isExpired(Instant referenceInstant) {
        if (this.expiresAtUtc == null) {
            return false;
        }
        Instant now = referenceInstant != null ? referenceInstant : Instant.now();
        return now.isAfter(this.expiresAtUtc);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public Instant getCreatedAtUtc() {
        return createdAtUtc;
    }

    public Instant getExpiresAtUtc() {
        return expiresAtUtc;
    }

    public Long getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Long accessCount) {
        this.accessCount = accessCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LinkEntity that)) return false;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}

