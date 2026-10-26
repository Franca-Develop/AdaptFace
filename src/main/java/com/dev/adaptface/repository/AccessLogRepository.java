package com.dev.adaptface.repository;

import com.dev.adaptface.entities.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    Page<AccessLog> findAllByOrderByLoggedAtDescIdDesc(Pageable pageable);

    Page<AccessLog> findByEnrollmentUserIdOrderByLoggedAtDescIdDesc(Long userId, Pageable pageable);
}
