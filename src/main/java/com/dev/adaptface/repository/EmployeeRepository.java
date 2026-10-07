package com.dev.adaptface.repository;

import com.dev.adaptface.entity.Employee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUserId(Long userId);
}
