package com.example.certificates.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A versioned link between a programme and the design it uses at a point in time.
 * Attaching a new design closes the current row (sets effectiveTo) and opens a new one.
 * Rows are never overwritten, so history is preserved forever.
 */
@Entity
@Table(name = "programme_design_versions")
public class ProgrammeDesignVersion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "programme_id", nullable = false)
    private UUID programmeId;

    @Column(name = "design_id", nullable = false)
    private UUID designId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    /** Null means this is the currently active version. */
    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProgrammeDesignVersion() {
    }

    public ProgrammeDesignVersion(UUID id, UUID programmeId, UUID designId, int version, Instant effectiveFrom) {
        this.id = id;
        this.programmeId = programmeId;
        this.designId = designId;
        this.version = version;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProgrammeId() {
        return programmeId;
    }

    public UUID getDesignId() {
        return designId;
    }

    public int getVersion() {
        return version;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void close(Instant when) {
        this.effectiveTo = when;
    }
}
