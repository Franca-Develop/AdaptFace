package com.dev.adaptface.repository;

import com.dev.adaptface.entities.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findFirstByNameIgnoreCase(String name);
}
