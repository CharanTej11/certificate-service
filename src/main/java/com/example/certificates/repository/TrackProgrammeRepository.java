package com.example.certificates.repository;

import com.example.certificates.entity.TrackProgramme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackProgrammeRepository extends JpaRepository<TrackProgramme, UUID> {

    List<TrackProgramme> findByTrackIdOrderByPositionAsc(UUID trackId);

    Optional<TrackProgramme> findByTrackIdAndProgrammeId(UUID trackId, UUID programmeId);

    boolean existsByTrackIdAndProgrammeId(UUID trackId, UUID programmeId);
}
