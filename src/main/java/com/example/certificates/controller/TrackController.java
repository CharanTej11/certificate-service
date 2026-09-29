package com.example.certificates.controller;

import com.example.certificates.dto.request.AddTrackProgrammeRequest;
import com.example.certificates.dto.request.CreateTrackRequest;
import com.example.certificates.dto.response.TrackResponse;
import com.example.certificates.service.TrackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tracks")
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @PostMapping
    public ResponseEntity<TrackResponse> create(@Valid @RequestBody CreateTrackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trackService.create(request));
    }

    @GetMapping
    public List<TrackResponse> list() {
        return trackService.list();
    }

    @GetMapping("/{id}")
    public TrackResponse get(@PathVariable UUID id) {
        return trackService.get(id);
    }

    @PostMapping("/{id}/programmes")
    public TrackResponse addProgramme(@PathVariable UUID id, @Valid @RequestBody AddTrackProgrammeRequest request) {
        return trackService.addProgramme(id, request);
    }

    @DeleteMapping("/{id}/programmes/{programmeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProgramme(@PathVariable UUID id, @PathVariable UUID programmeId) {
        trackService.removeProgramme(id, programmeId);
    }
}
