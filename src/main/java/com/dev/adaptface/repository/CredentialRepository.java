package com.dev.adaptface.repository;

import com.dev.adaptface.entity.Credential;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    List<Credential> findByEnrollmentActiveTrue();

    Optional<Credential> findByRfidTag(String rfidTag);
}
