package com.example.certificates.repository;

import com.example.certificates.entity.BulkCertificateJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BulkCertificateJobRepository extends JpaRepository<BulkCertificateJob, UUID> {
}
