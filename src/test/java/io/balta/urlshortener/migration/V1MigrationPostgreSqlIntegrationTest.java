package io.balta.urlshortener.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class V1MigrationPostgreSqlIntegrationTest {

    @Test
    @DisplayName("Deve aplicar migracao V1 com Flyway no PostgreSQL e validar unicidade case-sensitive do indice uq_links_code")
    void shouldApplyV1MigrationAndEnforceCaseSensitiveUniqueIndex() throws Exception {
        // Arrange
        boolean dockerAvailable = false;
        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ignored) {
            dockerAvailable = false;
        }

        Assumptions.assumeTrue(
                dockerAvailable,
                "Docker nao esta em execucao. O teste com Testcontainers sera executado quando o daemon do Docker estiver ativo."
        );

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("urlshortener")
                .withUsername("postgres")
                .withPassword("postgres")) {
            postgres.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .cleanDisabled(false)
                    .load();
            flyway.clean();
            flyway.migrate();

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

                String insertSql = "INSERT INTO links (code, target_url) VALUES (?, ?)";

                // Act & Assert (1): Inserir o primeiro link com codigo em minusculo
                try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                    stmt.setString(1, "balta-link");
                    stmt.setString(2, "https://balta.io");
                    int rows = stmt.executeUpdate();
                    assertThat(rows).isEqualTo(1);
                }

                // Act & Assert (2): Inserir link com codigo em maiusculo (deve coexistir pois o indice e case-sensitive)
                try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                    stmt.setString(1, "BALTA-LINK");
                    stmt.setString(2, "https://balta.io/maiusculo");
                    int rows = stmt.executeUpdate();
                    assertThat(rows).isEqualTo(1);
                }

                // Act & Assert (3): Validar que ambos os registros coexistem (case-sensitive)
                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT COUNT(*) FROM links WHERE code IN ('balta-link', 'BALTA-LINK')")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).isEqualTo(2);
                    }
                }

                // Act & Assert (4): Tentar inserir codigo duplicado identico deve falhar por violacao de chave unica
                assertThatThrownBy(() -> {
                    try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                        stmt.setString(1, "balta-link");
                        stmt.setString(2, "https://outro-site.com");
                        stmt.executeUpdate();
                    }
                }).isInstanceOf(SQLException.class)
                  .hasMessageContaining("uq_links_code");
            }
        }
    }
}

