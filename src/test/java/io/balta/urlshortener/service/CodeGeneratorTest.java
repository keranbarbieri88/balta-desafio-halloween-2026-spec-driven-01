package io.balta.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CodeGeneratorTest {

    private CodeGenerator codeGenerator;

    @BeforeEach
    void setUp() {
        codeGenerator = new CodeGenerator();
    }

    @Test
    @DisplayName("Deve gerar codigo com exatamente 7 caracteres")
    void shouldGenerateCodeWithExactLengthOfSeven() {
        // Arrange
        // (Preparado no setUp)

        // Act
        String code = codeGenerator.generate();

        // Assert
        assertThat(code).hasSize(7);
    }

    @Test
    @DisplayName("Deve conter apenas caracteres alfanumericos case-sensitive [a-zA-Z0-9]")
    void shouldContainOnlyAlphanumericCharacters() {
        // Arrange
        String alphanumericRegex = "^[a-zA-Z0-9]{7}$";

        // Act
        String code = codeGenerator.generate();

        // Assert
        assertThat(code).matches(alphanumericRegex);
    }

    @Test
    @DisplayName("Nao deve gerar palavras reservadas do sistema")
    void shouldNeverGenerateReservedTerms() {
        // Arrange
        Set<String> reservedTerms = CodeGenerator.getReservedTerms();

        // Act & Assert
        for (int i = 0; i < 200; i++) {
            String code = codeGenerator.generate();
            assertThat(reservedTerms).doesNotContain(code.toLowerCase());
        }
    }

    @Test
    @DisplayName("Deve identificar corretamente se um termo e reservado de forma case-insensitive")
    void shouldCorrectlyIdentifyReservedTerms() {
        // Arrange
        String reservedLower = "swagger";
        String reservedUpper = "SWAGGER";
        String reservedMixed = "SwAgGeR";
        String notReserved = "meuLink";

        // Act
        boolean isLowerReserved = codeGenerator.isReserved(reservedLower);
        boolean isUpperReserved = codeGenerator.isReserved(reservedUpper);
        boolean isMixedReserved = codeGenerator.isReserved(reservedMixed);
        boolean isNotReserved = codeGenerator.isReserved(notReserved);

        // Assert
        assertThat(isLowerReserved).isTrue();
        assertThat(isUpperReserved).isTrue();
        assertThat(isMixedReserved).isTrue();
        assertThat(isNotReserved).isFalse();
    }

    @Test
    @DisplayName("Deve gerar codigos variados com diferenciacao entre maiusculas e minusculas")
    void shouldGenerateDiverseCodesDemonstratingEntropy() {
        // Arrange
        Set<String> generatedCodes = new HashSet<>();
        int sampleSize = 100;

        // Act
        for (int i = 0; i < sampleSize; i++) {
            generatedCodes.add(codeGenerator.generate());
        }

        // Assert
        assertThat(generatedCodes).hasSize(sampleSize);
    }
}

