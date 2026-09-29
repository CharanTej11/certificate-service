package com.example.certificates.repository;

import com.example.certificates.entity.BulkCertificateJobItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BulkCertificateJobItemRepository extends JpaRepository<BulkCertificateJobItem, UUID> {

    List<BulkCertificateJobItem> findByJobId(UUID jobId);
}
