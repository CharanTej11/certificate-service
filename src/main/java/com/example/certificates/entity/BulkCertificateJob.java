package com.example.certificates.entity;

import com.example.certificates.enums.BulkJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A record of a bulk issue request. Each people entry becomes a job item.
 * Items are committed independently, so a crash leaves already-issued
 * certificates intact and the job resumable/reportable.
 */
@Entity
@Table(name = "bulk_certificate_jobs")
public class BulkCertificateJob {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "programme_id", nullable = false, updatable = false)
    private UUID programmeId;

    @Column(name = "track_id", updatable = false)
    private UUID trackId;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "issued_count", nullable = false)
    private int issuedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BulkJobStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected BulkCertificateJob() {
    }

    public BulkCertificateJob(UUID id, UUID programmeId, UUID trackId, int totalCount) {
        this.id = id;
        this.programmeId = programmeId;
        this.trackId = trackId;
        this.totalCount = totalCount;
        this.issuedCount = 0;
        this.failedCount = 0;
        this.createdAt = Instant.now();
    }

    public void complete(int issued, int failed) {
        this.issuedCount = issued;
        this.failedCount = failed;
        this.status = failed == 0 ? BulkJobStatus.COMPLETED : BulkJobStatus.COMPLETED_WITH_ERRORS;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProgrammeId() {
        return programmeId;
    }

    public UUID getTrackId() {
        return trackId;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getIssuedCount() {
        return issuedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public BulkJobStatus getStatus() {
        return status;
    }
}
