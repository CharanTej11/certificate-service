package com.example.certificates.repository;

import com.example.certificates.entity.ProgrammeDesignVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProgrammeDesignVersionRepository extends JpaRepository<ProgrammeDesignVersion, UUID> {

    /** The currently open version for a programme (effective_to is null). */
    Optional<ProgrammeDesignVersion> findByProgrammeIdAndEffectiveToIsNull(UUID programmeId);

    /**
     * The design version applicable at a given instant: effective_from <= at
     * and (effective_to is null or effective_to > at).
     * Answers "which design was this programme using on 5 March?".
     */
    @Query("""
            select v from ProgrammeDesignVersion v
            where v.programmeId = :programmeId
              and v.effectiveFrom <= :at
              and (v.effectiveTo is null or v.effectiveTo > :at)
            """)
    Optional<ProgrammeDesignVersion> findApplicableAt(@Param("programmeId") UUID programmeId,
                                                      @Param("at") Instant at);

    @Query("select coalesce(max(v.version), 0) from ProgrammeDesignVersion v where v.programmeId = :programmeId")
    int findMaxVersion(@Param("programmeId") UUID programmeId);
}
