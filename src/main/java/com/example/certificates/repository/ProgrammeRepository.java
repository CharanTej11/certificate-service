package com.example.certificates.repository;

import com.example.certificates.entity.Programme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProgrammeRepository extends JpaRepository<Programme, UUID> {
}
