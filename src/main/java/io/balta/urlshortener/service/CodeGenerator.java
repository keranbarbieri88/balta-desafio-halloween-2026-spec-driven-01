package io.balta.urlshortener.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Set;

@Component
public class CodeGenerator {

    private static final String ALPHANUMERIC_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 7;
    private static final Set<String> RESERVED_TERMS = Set.of(
            "api",
            "actuator",
            "swagger",
            "swagger-ui",
            "v3",
            "health"
    );

    private final SecureRandom secureRandom;

    public CodeGenerator() {
        this.secureRandom = new SecureRandom();
    }

    public CodeGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generate() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                int randomIndex = secureRandom.nextInt(ALPHANUMERIC_CHARACTERS.length());
                sb.append(ALPHANUMERIC_CHARACTERS.charAt(randomIndex));
            }
            code = sb.toString();
        } while (isReserved(code));

        return code;
    }

    public boolean isReserved(String code) {
        if (code == null) {
            return false;
        }
        return RESERVED_TERMS.contains(code.toLowerCase());
    }

    public static Set<String> getReservedTerms() {
        return RESERVED_TERMS;
    }
}

