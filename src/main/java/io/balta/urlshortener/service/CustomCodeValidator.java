package io.balta.urlshortener.service;

import io.balta.urlshortener.exception.InvalidCustomCodeException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CustomCodeValidator {

    private static final Pattern CUSTOM_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{4,20}$");
    private final CodeGenerator codeGenerator;

    public CustomCodeValidator(CodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    public void validate(String customCode) {
        if (customCode == null || customCode.isBlank()) {
            throw new InvalidCustomCodeException("O código personalizado não pode ser vazio.");
        }

        String trimmedCode = customCode.trim();

        if (codeGenerator.isReserved(trimmedCode)) {
            throw new InvalidCustomCodeException(
                    "O código '" + trimmedCode + "' coincide com um termo reservado do sistema e não pode ser utilizado."
            );
        }

        if (!CUSTOM_CODE_PATTERN.matcher(trimmedCode).matches()) {
            throw new InvalidCustomCodeException(
                    "O código personalizado deve conter entre 4 e 20 caracteres e incluir apenas letras, números, hífen ou sublinhado."
            );
        }
    }
}

