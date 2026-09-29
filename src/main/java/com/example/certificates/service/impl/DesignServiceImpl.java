package com.example.certificates.service.impl;

import com.example.certificates.dto.request.CreateDesignRequest;
import com.example.certificates.dto.response.DesignResponse;
import com.example.certificates.entity.Design;
import com.example.certificates.exception.NotFoundException;
import com.example.certificates.mapper.DtoMapper;
import com.example.certificates.repository.DesignRepository;
import com.example.certificates.service.DesignService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DesignServiceImpl implements DesignService {

    private final DesignRepository designRepository;
    private final DtoMapper mapper;

    public DesignServiceImpl(DesignRepository designRepository, DtoMapper mapper) {
        this.designRepository = designRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public DesignResponse create(CreateDesignRequest request) {
        Design design = new Design(UUID.randomUUID(), request.name(), request.content());
        return mapper.toDesignResponse(designRepository.save(design));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignResponse> list() {
        return designRepository.findAll().stream().map(mapper::toDesignResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DesignResponse get(UUID id) {
        return mapper.toDesignResponse(load(id));
    }

    @Override
    @Transactional
    public DesignResponse disable(UUID id) {
        Design design = load(id);
        design.disable();
        return mapper.toDesignResponse(design);
    }

    private Design load(UUID id) {
        return designRepository.findById(id).orElseThrow(() -> NotFoundException.of("Design", id));
    }
}
