package com.dev.adaptface;

import static org.assertj.core.api.Assertions.assertThat;

import com.dev.adaptface.dto.request.BootstrapRequestDTO;
import com.dev.adaptface.entity.enums.UserType;
import com.dev.adaptface.exceptions.BusinessException;
import com.dev.adaptface.repository.UserRepository;
import com.dev.adaptface.service.AuthService;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EnabledIfEnvironmentVariable(named = "AUDIT_MYSQL_URL", matches = "jdbc:mysql://127\\.0\\.0\\.1:3306/adaptface_audit_[a-f0-9]+.*")
class BootstrapConcurrencyMySqlTests {

    @Autowired private AuthService auth;
    @Autowired private UserRepository users;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        MySqlAuditIntegrationTests.mysqlProperties(registry);
    }

    @Test
    void shouldCommitOnlyOneInitialAdministratorUnderConcurrentCalls() throws Exception {
        assertThat(auth.requiresSetup()).isTrue();
        var start = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = bootstrapAttempt(start, "60000000001", "bootstrap-a@example.test");
            Callable<Boolean> second = bootstrapAttempt(start, "60000000002", "bootstrap-b@example.test");
            var one = executor.submit(first);
            var two = executor.submit(second);
            int successes = (one.get(30, TimeUnit.SECONDS) ? 1 : 0) + (two.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertThat(successes).isEqualTo(1);
            assertThat(users.findAll().stream().filter(user -> user.getUserType() == UserType.EMPLOYEE).count())
                    .isEqualTo(1);
            assertThat(auth.requiresSetup()).isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Boolean> bootstrapAttempt(CyclicBarrier start, String cpf, String email) {
        return () -> {
            start.await(10, TimeUnit.SECONDS);
            BootstrapRequestDTO request = new BootstrapRequestDTO();
            request.setInstitutionName("Auditoria de concorrência sintética");
            request.setName("Administrador sintético concorrente");
            request.setCpf(cpf);
            request.setEmail(email);
            request.setPassword("synthetic-password");
            request.setDepartment("Testes");
            try {
                auth.bootstrap("test-bootstrap-key-with-at-least-32-characters", request);
                return true;
            } catch (BusinessException | DataAccessException exception) {
                return false;
            }
        };
    }
}
