package com.example.certificates.service.impl;

import com.example.certificates.dto.request.CreatePersonRequest;
import com.example.certificates.dto.response.PersonResponse;
import com.example.certificates.entity.Person;
import com.example.certificates.exception.NotFoundException;
import com.example.certificates.mapper.DtoMapper;
import com.example.certificates.repository.PersonRepository;
import com.example.certificates.service.PersonService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final DtoMapper mapper;

    public PersonServiceImpl(PersonRepository personRepository, DtoMapper mapper) {
        this.personRepository = personRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PersonResponse create(CreatePersonRequest request) {
        Person person = new Person(UUID.randomUUID(), request.name());
        return mapper.toPersonResponse(personRepository.save(person));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonResponse get(UUID id) {
        return personRepository.findById(id)
                .map(mapper::toPersonResponse)
                .orElseThrow(() -> NotFoundException.of("Person", id));
    }
}
