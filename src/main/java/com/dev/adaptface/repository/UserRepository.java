package com.dev.adaptface.repository;

import com.dev.adaptface.entity.User;
import com.dev.adaptface.entity.enums.UserType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByUserType(UserType userType);
}
