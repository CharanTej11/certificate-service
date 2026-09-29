package com.example.certificates.controller;

import com.example.certificates.seed.DataSeedService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Loads the demo dataset. Exposed for the take-home so a reviewer can populate a
 * clean database with one call. In production this would be removed or guarded.
 */
@RestController
@RequestMapping("/api/v1/dev")
public class DevSeedController {

    private final DataSeedService dataSeedService;

    public DevSeedController(DataSeedService dataSeedService) {
        this.dataSeedService = dataSeedService;
    }

    @PostMapping("/seed")
    public Map<String, Object> seed() {
        return dataSeedService.seed();
    }
}
