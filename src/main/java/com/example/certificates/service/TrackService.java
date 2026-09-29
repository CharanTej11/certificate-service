package com.example.certificates.service;

import com.example.certificates.dto.request.AddTrackProgrammeRequest;
import com.example.certificates.dto.request.CreateTrackRequest;
import com.example.certificates.dto.response.TrackResponse;

import java.util.List;
import java.util.UUID;

public interface TrackService {

    TrackResponse create(CreateTrackRequest request);

    List<TrackResponse> list();

    TrackResponse get(UUID id);

    TrackResponse addProgramme(UUID trackId, AddTrackProgrammeRequest request);

    void removeProgramme(UUID trackId, UUID programmeId);
}
