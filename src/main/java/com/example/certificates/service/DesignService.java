package com.example.certificates.service;

import com.example.certificates.dto.request.CreateDesignRequest;
import com.example.certificates.dto.response.DesignResponse;

import java.util.List;
import java.util.UUID;

public interface DesignService {

    DesignResponse create(CreateDesignRequest request);

    List<DesignResponse> list();

    DesignResponse get(UUID id);

    DesignResponse disable(UUID id);
}
