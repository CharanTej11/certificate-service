package com.example.certificates.service;

import com.example.certificates.dto.request.CreatePersonRequest;
import com.example.certificates.dto.response.PersonResponse;

import java.util.UUID;

public interface PersonService {

    PersonResponse create(CreatePersonRequest request);

    PersonResponse get(UUID id);
}
