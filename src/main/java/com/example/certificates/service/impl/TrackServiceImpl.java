package com.example.certificates.service.impl;

import com.example.certificates.dto.request.AddTrackProgrammeRequest;
import com.example.certificates.dto.request.CreateTrackRequest;
import com.example.certificates.dto.response.TrackProgrammeResponse;
import com.example.certificates.dto.response.TrackResponse;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.Track;
import com.example.certificates.entity.TrackProgramme;
import com.example.certificates.exception.BusinessRuleException;
import com.example.certificates.exception.NotFoundException;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.repository.TrackProgrammeRepository;
import com.example.certificates.repository.TrackRepository;
import com.example.certificates.service.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TrackServiceImpl implements TrackService {

    private static final Logger log = LoggerFactory.getLogger(TrackServiceImpl.class);

    private final TrackRepository trackRepository;
    private final TrackProgrammeRepository trackProgrammeRepository;
    private final ProgrammeRepository programmeRepository;

    public TrackServiceImpl(TrackRepository trackRepository,
                            TrackProgrammeRepository trackProgrammeRepository,
                            ProgrammeRepository programmeRepository) {
        this.trackRepository = trackRepository;
        this.trackProgrammeRepository = trackProgrammeRepository;
        this.programmeRepository = programmeRepository;
    }

    @Override
    @Transactional
    public TrackResponse create(CreateTrackRequest request) {
        Track track = new Track(UUID.randomUUID(), request.name(), request.description());
        trackRepository.save(track);
        return toResponse(track);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackResponse> list() {
        return trackRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TrackResponse get(UUID id) {
        return toResponse(loadTrack(id));
    }

    @Override
    @Transactional
    public TrackResponse addProgramme(UUID trackId, AddTrackProgrammeRequest request) {
        Track track = loadTrack(trackId);
        if (!programmeRepository.existsById(request.programmeId())) {
            throw NotFoundException.of("Programme", request.programmeId());
        }
        if (trackProgrammeRepository.existsByTrackIdAndProgrammeId(trackId, request.programmeId())) {
            throw new BusinessRuleException("Programme already belongs to this track");
        }
        int position = request.position() != null
                ? request.position()
                : trackProgrammeRepository.findByTrackIdOrderByPositionAsc(trackId).size();
        try {
            trackProgrammeRepository.save(
                    new TrackProgramme(UUID.randomUUID(), trackId, request.programmeId(), position));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleException("Programme already belongs to this track");
        }
        return toResponse(track);
    }

    @Override
    @Transactional
    public void removeProgramme(UUID trackId, UUID programmeId) {
        loadTrack(trackId);
        TrackProgramme link = trackProgrammeRepository.findByTrackIdAndProgrammeId(trackId, programmeId)
                .orElseThrow(() -> new NotFoundException(
                        "Programme " + programmeId + " is not in track " + trackId));
        // Removing membership does not touch certificates already earned through this track.
        trackProgrammeRepository.delete(link);
        log.info("Removed programme {} from track {}", programmeId, trackId);
    }

    private Track loadTrack(UUID id) {
        return trackRepository.findById(id).orElseThrow(() -> NotFoundException.of("Track", id));
    }

    private TrackResponse toResponse(Track track) {
        List<TrackProgramme> links = trackProgrammeRepository.findByTrackIdOrderByPositionAsc(track.getId());
        Map<UUID, String> names = programmeRepository.findAllById(
                        links.stream().map(TrackProgramme::getProgrammeId).toList()).stream()
                .collect(Collectors.toMap(Programme::getId, Programme::getName));
        List<TrackProgrammeResponse> programmes = links.stream()
                .map(l -> new TrackProgrammeResponse(
                        l.getProgrammeId(),
                        names.getOrDefault(l.getProgrammeId(), null),
                        l.getPosition()))
                .toList();
        return new TrackResponse(track.getId(), track.getName(), track.getDescription(),
                track.isActive(), programmes, track.getCreatedAt());
    }
}
