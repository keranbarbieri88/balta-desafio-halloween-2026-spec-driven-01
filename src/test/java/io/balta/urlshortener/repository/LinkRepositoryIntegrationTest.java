package io.balta.urlshortener.repository;

import io.balta.urlshortener.domain.LinkEntity;
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
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class LinkRepositoryIntegrationTest {

    @Test
    @DisplayName("Deve garantir incremento atomico concorrente sem perdas no PostgreSQL (T-11, RF-05, D-02)")
    void shouldIncrementAccessCountConcurrentlyWithoutLostUpdates() throws Exception {
        // Arrange
        boolean dockerAvailable = false;
        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ignored) {
            dockerAvailable = false;
        }

        Assumptions.assumeTrue(
                dockerAvailable,
                "Docker nao esta ativo. O teste de concorrencia no PostgreSQL sera executado quando o daemon do Docker estiver em execucao."
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

            String linkCode = "concorrente-test";
            try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO links (code, target_url, access_count) VALUES (?, ?, 0)")) {
                stmt.setString(1, linkCode);
                stmt.setString(2, "https://balta.io");
                stmt.executeUpdate();
            }

            int numberOfThreads = 20;
            int incrementsPerThread = 10;
            int totalExpectedIncrements = numberOfThreads * incrementsPerThread;

            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

            // Act
            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        try (Connection threadConn = DriverManager.getConnection(
                                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                             PreparedStatement updateStmt = threadConn.prepareStatement(
                                     "UPDATE links SET access_count = access_count + 1 WHERE code = ?")) {
                            updateStmt.setString(1, linkCode);
                            for (int j = 0; j < incrementsPerThread; j++) {
                                updateStmt.executeUpdate();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown(); // Libera todas as threads simultaneamente
            finishLatch.await();    // Aguarda término de todas as threads
            executor.shutdown();

            // Assert
            try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 PreparedStatement checkStmt = conn.prepareStatement("SELECT access_count FROM links WHERE code = ?")) {
                checkStmt.setString(1, linkCode);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    long finalCount = rs.getLong(1);
                    assertThat(finalCount).isEqualTo(totalExpectedIncrements);
                }
            }
        }
    }
}

