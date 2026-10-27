package io.balta.urlshortener.service;

import io.balta.urlshortener.exception.InvalidCustomCodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomCodeValidatorTest {

    private CustomCodeValidator validator;

    @BeforeEach
    void setUp() {
        CodeGenerator codeGenerator = new CodeGenerator();
        validator = new CustomCodeValidator(codeGenerator);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "balta",
            "meu-link",
            "promo_2026",
            "ArtigoTop10",
            "1234",
            "codigo-com-20-chars_"
    })
    @DisplayName("Deve aceitar codigos personalizados validos entre 4 e 20 caracteres")
    void shouldAcceptValidCustomCodes(String validCode) {
        // Arrange
        // (Parametrizado)

        // Act & Assert
        assertThatCode(() -> validator.validate(validCode))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",          // 3 chars (< 4)
            "ab",           // 2 chars
            "a",            // 1 char
            "codigo-muito-longo-com-mais-de-20-caracteres" // > 20 chars
    })
    @DisplayName("Deve rejeitar codigos menores que 4 ou maiores que 20 caracteres")
    void shouldRejectCodesOutOfLengthBounds(String invalidLengthCode) {
        // Arrange
        // (Parametrizado)

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(invalidLengthCode))
                .isInstanceOf(InvalidCustomCodeException.class)
                .hasMessageContaining("entre 4 e 20 caracteres");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "codigo com espaco",
            "codigo.com.ponto",
            "link@promo",
            "link#1",
            "link/barra",
            "link$cifrao"
    })
    @DisplayName("Deve rejeitar codigos contendo caracteres especiais nao permitidos")
    void shouldRejectSpecialCharacters(String invalidCharsCode) {
        // Arrange
        // (Parametrizado)

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(invalidCharsCode))
                .isInstanceOf(InvalidCustomCodeException.class)
                .hasMessageContaining("apenas letras, números, hífen ou sublinhado");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "api",
            "API",
            "actuator",
            "ACTUATOR",
            "swagger",
            "Swagger",
            "swagger-ui",
            "health",
            "HEALTH"
    })
    @DisplayName("Deve rejeitar palavras reservadas do sistema mesmo com casing diferente")
    void shouldRejectReservedTermsRegardlessOfCasing(String reservedTerm) {
        // Arrange
        // (Parametrizado)

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(reservedTerm))
                .isInstanceOf(InvalidCustomCodeException.class)
                .hasMessageContaining("termo reservado");
    }

    @Test
    @DisplayName("Deve rejeitar codigo nulo ou vazio")
    void shouldRejectNullOrBlank() {
        // Arrange
        String nullCode = null;
        String blankCode = "   ";

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(nullCode))
                .isInstanceOf(InvalidCustomCodeException.class)
                .hasMessageContaining("não pode ser vazio");

        assertThatThrownBy(() -> validator.validate(blankCode))
                .isInstanceOf(InvalidCustomCodeException.class)
                .hasMessageContaining("não pode ser vazio");
    }
}

