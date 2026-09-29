package com.example.certificates.service.impl;

import com.example.certificates.dto.request.AttachDesignRequest;
import com.example.certificates.dto.request.CreateProgrammeRequest;
import com.example.certificates.dto.request.UpdateProgrammeRequest;
import com.example.certificates.dto.response.ProgrammeResponse;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.ProgrammeDesignVersion;
import com.example.certificates.exception.NotFoundException;
import com.example.certificates.mapper.DtoMapper;
import com.example.certificates.repository.DesignRepository;
import com.example.certificates.repository.ProgrammeDesignVersionRepository;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.service.ProgrammeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProgrammeServiceImpl implements ProgrammeService {

    private static final Logger log = LoggerFactory.getLogger(ProgrammeServiceImpl.class);

    private final ProgrammeRepository programmeRepository;
    private final DesignRepository designRepository;
    private final ProgrammeDesignVersionRepository versionRepository;
    private final DtoMapper mapper;

    public ProgrammeServiceImpl(ProgrammeRepository programmeRepository,
                                DesignRepository designRepository,
                                ProgrammeDesignVersionRepository versionRepository,
                                DtoMapper mapper) {
        this.programmeRepository = programmeRepository;
        this.designRepository = designRepository;
        this.versionRepository = versionRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ProgrammeResponse create(CreateProgrammeRequest request) {
        Programme programme = new Programme(UUID.randomUUID(), request.name(), request.description());
        return mapper.toProgrammeResponse(programmeRepository.save(programme));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgrammeResponse> list() {
        return programmeRepository.findAll().stream().map(mapper::toProgrammeResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProgrammeResponse get(UUID id) {
        return mapper.toProgrammeResponse(load(id));
    }

    @Override
    @Transactional
    public ProgrammeResponse update(UUID id, UpdateProgrammeRequest request) {
        Programme programme = load(id);
        if (request.name() != null) {
            programme.setName(request.name());
        }
        if (request.description() != null) {
            programme.setDescription(request.description());
        }
        // Renaming never touches certificates — they hold their own name snapshot.
        return mapper.toProgrammeResponse(programme);
    }

    @Override
    @Transactional
    public ProgrammeResponse disable(UUID id) {
        Programme programme = load(id);
        programme.disable();
        return mapper.toProgrammeResponse(programme);
    }

    @Override
    @Transactional
    public void attachDesign(UUID programmeId, AttachDesignRequest request) {
        load(programmeId);
        if (!designRepository.existsById(request.designId())) {
            throw NotFoundException.of("Design", request.designId());
        }
        Instant now = Instant.now();

        Optional<ProgrammeDesignVersion> current =
                versionRepository.findByProgrammeIdAndEffectiveToIsNull(programmeId);
        current.ifPresent(v -> v.close(now));

        int nextVersion = versionRepository.findMaxVersion(programmeId) + 1;
        ProgrammeDesignVersion newVersion = new ProgrammeDesignVersion(
                UUID.randomUUID(), programmeId, request.designId(), nextVersion, now);
        versionRepository.save(newVersion);

        log.info("Attached design {} to programme {} as version {}",
                request.designId(), programmeId, nextVersion);
    }

    private Programme load(UUID id) {
        return programmeRepository.findById(id).orElseThrow(() -> NotFoundException.of("Programme", id));
    }
}
