package com.dev.adaptface.repository;

import com.dev.adaptface.entities.Credential;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    List<Credential> findByEnrollmentActiveTrue();

    @Query("select c from Credential c join fetch c.enrollment e join fetch e.user where c.faceEmbedding is not null")
    List<Credential> findFacialCredentials();

    @Query("select c from Credential c join fetch c.enrollment e join fetch e.user where c.accessCode is not null")
    List<Credential> findCodeCredentials();

    Optional<Credential> findByEnrollmentId(Long enrollmentId);

    Optional<Credential> findByRfidTag(String rfidTag);
}
