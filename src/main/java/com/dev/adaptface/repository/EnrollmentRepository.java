package com.dev.adaptface.repository;

import com.dev.adaptface.entity.Enrollment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserId(Long userId);
}
