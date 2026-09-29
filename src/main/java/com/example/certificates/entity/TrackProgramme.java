package com.example.certificates.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Membership of a programme in a track, with an ordering position.
 * Unique on (track_id, programme_id). Removing a row does not touch certificates.
 */
@Entity
@Table(name = "track_programmes")
public class TrackProgramme {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "track_id", nullable = false)
    private UUID trackId;

    @Column(name = "programme_id", nullable = false)
    private UUID programmeId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TrackProgramme() {
    }

    public TrackProgramme(UUID id, UUID trackId, UUID programmeId, int position) {
        this.id = id;
        this.trackId = trackId;
        this.programmeId = programmeId;
        this.position = position;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTrackId() {
        return trackId;
    }

    public UUID getProgrammeId() {
        return programmeId;
    }

    public int getPosition() {
        return position;
    }
}
