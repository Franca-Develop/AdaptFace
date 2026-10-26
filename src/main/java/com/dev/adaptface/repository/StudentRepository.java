package com.dev.adaptface.repository;

import com.dev.adaptface.entities.Student;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);
}
