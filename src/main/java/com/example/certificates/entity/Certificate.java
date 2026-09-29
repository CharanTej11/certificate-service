package com.example.certificates.entity;

import com.example.certificates.enums.CertificateStatus;
import com.example.certificates.enums.RouteType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An issued certificate. Everything an employer reads comes from the *_snapshot
 * columns captured at issue time. Nothing here is ever re-derived from live
 * programme / design / track / person rows, so the certificate never changes.
 */
@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Public, non-sequential, URL-safe identifier printed on the certificate. */
    @Column(name = "public_code", nullable = false, unique = true, updatable = false)
    private String publicCode;

    // --- references (kept for joins/analytics only, never for display) ---
    @Column(name = "person_id", nullable = false, updatable = false)
    private UUID personId;

    @Column(name = "programme_id", nullable = false, updatable = false)
    private UUID programmeId;

    @Column(name = "track_id", updatable = false)
    private UUID trackId;

    @Column(name = "design_id", updatable = false)
    private UUID designId;

    @Column(name = "programme_design_version_id", updatable = false)
    private UUID programmeDesignVersionId;

    // --- immutable snapshots (the only source of truth for display) ---
    @Column(name = "person_name_snapshot", nullable = false, updatable = false)
    private String personNameSnapshot;

    @Column(name = "programme_name_snapshot", nullable = false, updatable = false)
    private String programmeNameSnapshot;

    @Column(name = "programme_description_snapshot", updatable = false)
    private String programmeDescriptionSnapshot;

    @Column(name = "design_name_snapshot", updatable = false)
    private String designNameSnapshot;

    @Column(name = "design_content_snapshot", updatable = false, columnDefinition = "text")
    private String designContentSnapshot;

    @Column(name = "track_name_snapshot", updatable = false)
    private String trackNameSnapshot;

    // --- routing / uniqueness ---
    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", nullable = false, updatable = false)
    private RouteType routeType;

    /** Normalized route key: "OWN" for no-track, else the track UUID as text. */
    @Column(name = "route_key", nullable = false, updatable = false)
    private String routeKey;

    // --- lifecycle ---
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CertificateStatus status;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Certificate() {
    }

    private Certificate(Builder b) {
        this.id = b.id;
        this.publicCode = b.publicCode;
        this.personId = b.personId;
        this.programmeId = b.programmeId;
        this.trackId = b.trackId;
        this.designId = b.designId;
        this.programmeDesignVersionId = b.programmeDesignVersionId;
        this.personNameSnapshot = b.personNameSnapshot;
        this.programmeNameSnapshot = b.programmeNameSnapshot;
        this.programmeDescriptionSnapshot = b.programmeDescriptionSnapshot;
        this.designNameSnapshot = b.designNameSnapshot;
        this.designContentSnapshot = b.designContentSnapshot;
        this.trackNameSnapshot = b.trackNameSnapshot;
        this.routeType = b.routeType;
        this.routeKey = b.routeKey;
        this.issuedAt = b.issuedAt;
        this.status = CertificateStatus.LIVE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void cancel(String reason, Instant when) {
        this.status = CertificateStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = when;
    }

    public UUID getId() {
        return id;
    }

    public String getPublicCode() {
        return publicCode;
    }

    public UUID getPersonId() {
        return personId;
    }

    public UUID getProgrammeId() {
        return programmeId;
    }

    public UUID getTrackId() {
        return trackId;
    }

    public UUID getDesignId() {
        return designId;
    }

    public UUID getProgrammeDesignVersionId() {
        return programmeDesignVersionId;
    }

    public String getPersonNameSnapshot() {
        return personNameSnapshot;
    }

    public String getProgrammeNameSnapshot() {
        return programmeNameSnapshot;
    }

    public String getProgrammeDescriptionSnapshot() {
        return programmeDescriptionSnapshot;
    }

    public String getDesignNameSnapshot() {
        return designNameSnapshot;
    }

    public String getDesignContentSnapshot() {
        return designContentSnapshot;
    }

    public String getTrackNameSnapshot() {
        return trackNameSnapshot;
    }

    public RouteType getRouteType() {
        return routeType;
    }

    public String getRouteKey() {
        return routeKey;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public CertificateStatus getStatus() {
        return status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public static final class Builder {
        private UUID id;
        private String publicCode;
        private UUID personId;
        private UUID programmeId;
        private UUID trackId;
        private UUID designId;
        private UUID programmeDesignVersionId;
        private String personNameSnapshot;
        private String programmeNameSnapshot;
        private String programmeDescriptionSnapshot;
        private String designNameSnapshot;
        private String designContentSnapshot;
        private String trackNameSnapshot;
        private RouteType routeType;
        private String routeKey;
        private Instant issuedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder publicCode(String publicCode) {
            this.publicCode = publicCode;
            return this;
        }

        public Builder personId(UUID personId) {
            this.personId = personId;
            return this;
        }

        public Builder programmeId(UUID programmeId) {
            this.programmeId = programmeId;
            return this;
        }

        public Builder trackId(UUID trackId) {
            this.trackId = trackId;
            return this;
        }

        public Builder designId(UUID designId) {
            this.designId = designId;
            return this;
        }

        public Builder programmeDesignVersionId(UUID programmeDesignVersionId) {
            this.programmeDesignVersionId = programmeDesignVersionId;
            return this;
        }

        public Builder personNameSnapshot(String v) {
            this.personNameSnapshot = v;
            return this;
        }

        public Builder programmeNameSnapshot(String v) {
            this.programmeNameSnapshot = v;
            return this;
        }

        public Builder programmeDescriptionSnapshot(String v) {
            this.programmeDescriptionSnapshot = v;
            return this;
        }

        public Builder designNameSnapshot(String v) {
            this.designNameSnapshot = v;
            return this;
        }

        public Builder designContentSnapshot(String v) {
            this.designContentSnapshot = v;
            return this;
        }

        public Builder trackNameSnapshot(String v) {
            this.trackNameSnapshot = v;
            return this;
        }

        public Builder routeType(RouteType routeType) {
            this.routeType = routeType;
            return this;
        }

        public Builder routeKey(String routeKey) {
            this.routeKey = routeKey;
            return this;
        }

        public Builder issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Certificate build() {
            return new Certificate(this);
        }
    }
}
