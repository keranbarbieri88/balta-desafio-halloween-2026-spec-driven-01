package io.balta.urlshortener.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class V1MigrationScriptUnitTest {

    @Test
    @DisplayName("Deve conter definio completa da tabela links e indice unico case-sensitive")
    void shouldContainCompleteDefinitionForLinksTableAndUniqueIndex() throws IOException {
        // Arrange
        ClassPathResource resource = new ClassPathResource("db/migration/V1__create_links_table.sql");

        // Act
        String sql = resource.getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(resource.exists())
                .as("O arquivo de migracao V1 deve existir no classpath db/migration")
                .isTrue();

        assertThat(sql)
                .as("Deve criar a tabela links")
                .containsIgnoringCase("CREATE TABLE links");

        assertThat(sql)
                .as("Deve conter coluna id com chave primaria")
                .containsIgnoringCase("id BIGSERIAL PRIMARY KEY");

        assertThat(sql)
                .as("Deve conter coluna code VARCHAR(20) NOT NULL")
                .containsIgnoringCase("code VARCHAR(20) NOT NULL");

        assertThat(sql)
                .as("Deve conter coluna target_url VARCHAR(2048) NOT NULL")
                .containsIgnoringCase("target_url VARCHAR(2048) NOT NULL");

        assertThat(sql)
                .as("Deve conter coluna created_at_utc com valor padrao em UTC")
                .containsIgnoringCase("created_at_utc TIMESTAMP WITH TIME ZONE NOT NULL");

        assertThat(sql)
                .as("Deve conter coluna expires_at_utc anulavel")
                .containsIgnoringCase("expires_at_utc TIMESTAMP WITH TIME ZONE NULL");

        assertThat(sql)
                .as("Deve conter coluna access_count com padrao 0")
                .containsIgnoringCase("access_count BIGINT NOT NULL DEFAULT 0");

        assertThat(sql)
                .as("Deve criar indice unico na coluna code para garantir unicidade e case-sensitivity")
                .containsIgnoringCase("CREATE UNIQUE INDEX uq_links_code ON links (code)");
    }
}

