package com.example.certificates.entity;

import com.example.certificates.enums.BulkItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bulk_certificate_job_items")
public class BulkCertificateJobItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "person_id", updatable = false)
    private UUID personId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BulkItemStatus status;

    @Column(name = "certificate_id")
    private UUID certificateId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BulkCertificateJobItem() {
    }

    public BulkCertificateJobItem(UUID id, UUID jobId, UUID personId,
                                  BulkItemStatus status, UUID certificateId, String reason) {
        this.id = id;
        this.jobId = jobId;
        this.personId = personId;
        this.status = status;
        this.certificateId = certificateId;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public BulkItemStatus getStatus() {
        return status;
    }

    public UUID getCertificateId() {
        return certificateId;
    }

    public String getReason() {
        return reason;
    }
}
