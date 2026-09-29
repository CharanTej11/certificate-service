package com.example.certificates.service;

import com.example.certificates.dto.request.BulkIssueRequest;
import com.example.certificates.dto.response.BulkIssueResponse;

public interface BulkCertificateService {

    BulkIssueResponse bulkIssue(BulkIssueRequest request);
}
