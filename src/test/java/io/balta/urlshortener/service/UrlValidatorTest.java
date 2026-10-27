package io.balta.urlshortener.service;

import io.balta.urlshortener.exception.InvalidUrlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlValidatorTest {

    private UrlValidator urlValidator;

    @BeforeEach
    void setUp() {
        urlValidator = new UrlValidator("localhost,127.0.0.1,::1,meuencurtador.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://exemplo.com",
            "https://balta.io",
            "https://google.com/search?q=spec+driven+development",
            "https://sub.dominio.org/caminho/completo?param=1&outro=2#secao"
    })
    @DisplayName("Deve aceitar URLs validas com HTTP e HTTPS")
    void shouldAcceptValidHttpAndHttpsUrls(String validUrl) {
        // Arrange
        // (Preparado no parameterized test)

        // Act & Assert
        assertThatCode(() -> urlValidator.validate(validUrl))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar URL nula ou em branco")
    void shouldRejectNullOrBlankUrl() {
        // Arrange
        String nullUrl = null;
        String blankUrl = "   ";

        // Act & Assert
        assertThatThrownBy(() -> urlValidator.validate(nullUrl))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("obrigatória");

        assertThatThrownBy(() -> urlValidator.validate(blankUrl))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("obrigatória");
    }

    @Test
    @DisplayName("Deve rejeitar URL com mais de 2048 caracteres")
    void shouldRejectUrlExceeding2048Characters() {
        // Arrange
        String longUrl = "https://exemplo.com/" + "a".repeat(2035);

        // Act & Assert
        assertThatThrownBy(() -> urlValidator.validate(longUrl))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("2048 caracteres");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://ftp.exemplo.com/arquivo.zip",
            "mailto:contato@exemplo.com",
            "file:///C:/arquivo.txt",
            "javascript:alert('xss')",
            "exemplo.com/sem-protocolo"
    })
    @DisplayName("Deve rejeitar esquemas e protocolos que nao sejam HTTP ou HTTPS")
    void shouldRejectNonHttpOrHttpsSchemes(String invalidProtocolUrl) {
        // Arrange
        // (Parametrizado)

        // Act & Assert
        assertThatThrownBy(() -> urlValidator.validate(invalidProtocolUrl))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("HTTP ou HTTPS");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost/artigo",
            "https://localhost:8080/api/links",
            "http://127.0.0.1/teste",
            "https://127.0.0.1:8080/redireciona",
            "https://meuencurtador.com/codigo"
    })
    @DisplayName("Deve bloquear auto-referencia e hosts configurados para prevenir loops")
    void shouldBlockSelfReferenceAndConfiguredHosts(String selfReferencingUrl) {
        // Arrange
        // (Parametrizado)

        // Act & Assert
        assertThatThrownBy(() -> urlValidator.validate(selfReferencingUrl))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("próprio domínio da aplicação");
    }
}

