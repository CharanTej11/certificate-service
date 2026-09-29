package com.example.certificates.repository;

import com.example.certificates.entity.ProgrammeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProgrammeRuleRepository extends JpaRepository<ProgrammeRule, UUID> {

    Optional<ProgrammeRule> findByProgrammeId(UUID programmeId);
}
