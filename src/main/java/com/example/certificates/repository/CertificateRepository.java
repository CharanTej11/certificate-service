package com.example.certificates.repository;

import com.example.certificates.entity.Certificate;
import com.example.certificates.enums.CertificateStatus;
import com.example.certificates.enums.RouteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findByPublicCode(String publicCode);

    Page<Certificate> findByPersonIdOrderByIssuedAtDesc(UUID personId, Pageable pageable);

    boolean existsByPersonIdAndProgrammeIdAndRouteTypeAndRouteKeyAndStatus(
            UUID personId, UUID programmeId, RouteType routeType, String routeKey, CertificateStatus status);
}
