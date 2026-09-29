package com.example.certificates.repository;

import com.example.certificates.entity.Design;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DesignRepository extends JpaRepository<Design, UUID> {
}
