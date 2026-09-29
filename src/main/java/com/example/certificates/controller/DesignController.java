package com.example.certificates.controller;

import com.example.certificates.dto.request.CreateDesignRequest;
import com.example.certificates.dto.response.DesignResponse;
import com.example.certificates.service.DesignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/designs")
public class DesignController {

    private final DesignService designService;

    public DesignController(DesignService designService) {
        this.designService = designService;
    }

    @PostMapping
    public ResponseEntity<DesignResponse> create(@Valid @RequestBody CreateDesignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(designService.create(request));
    }

    @GetMapping
    public List<DesignResponse> list() {
        return designService.list();
    }

    @GetMapping("/{id}")
    public DesignResponse get(@PathVariable UUID id) {
        return designService.get(id);
    }

    @PostMapping("/{id}/disable")
    public DesignResponse disable(@PathVariable UUID id) {
        return designService.disable(id);
    }
}
