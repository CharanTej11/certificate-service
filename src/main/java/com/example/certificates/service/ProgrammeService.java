package com.example.certificates.service;

import com.example.certificates.dto.request.AttachDesignRequest;
import com.example.certificates.dto.request.CreateProgrammeRequest;
import com.example.certificates.dto.request.UpdateProgrammeRequest;
import com.example.certificates.dto.response.ProgrammeResponse;

import java.util.List;
import java.util.UUID;

public interface ProgrammeService {

    ProgrammeResponse create(CreateProgrammeRequest request);

    List<ProgrammeResponse> list();

    ProgrammeResponse get(UUID id);

    ProgrammeResponse update(UUID id, UpdateProgrammeRequest request);

    ProgrammeResponse disable(UUID id);

    /**
     * Attach a design to a programme. Closes the current version and opens a new one.
     * Historical versions are preserved so we can always answer which design applied
     * at any past instant.
     */
    void attachDesign(UUID programmeId, AttachDesignRequest request);
}
