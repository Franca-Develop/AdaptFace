package com.dev.adaptface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.ServerSocket;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnabledIfEnvironmentVariable(named = "AUDIT_MYSQL_URL", matches = "jdbc:mysql://127\\.0\\.0\\.1:3306/adaptface_audit_[a-f0-9]+.*")
class MySqlAuditIntegrationTests extends SecurityAuditIntegrationTests {

    @Autowired private DataSource dataSource;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        String url = System.getenv("AUDIT_MYSQL_URL");
        if (url == null || !url.matches("jdbc:mysql://127\\.0\\.0\\.1:3306/adaptface_audit_[a-f0-9]+(\\?.*)?")) {
            throw new IllegalStateException("Use somente um esquema MySQL de auditoria isolado");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getenv("AUDIT_MYSQL_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("AUDIT_MYSQL_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Test
    void shouldUseActualMySqlAndRejectUnresponsiveConnection() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).startsWith("adaptface_audit_");
        }
        try (ServerSocket unresponsive = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            String url = "jdbc:mysql://127.0.0.1:" + unresponsive.getLocalPort()
                    + "/synthetic?connectTimeout=500&socketTimeout=500&useSSL=false";
            assertThrows(SQLException.class, () -> DriverManager.getConnection(url, "synthetic", "synthetic"));
        }
    }
}
