package io.balta.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateLinkRequest(
        @NotBlank(message = "A URL de destino é obrigatória.")
        @Size(max = 2048, message = "A URL de destino deve conter no máximo 2048 caracteres.")
        @Schema(description = "URL original que será encurtada", example = "https://balta.io/artigos/sdd-com-ia")
        String targetUrl,

        @Size(min = 4, max = 20, message = "O código personalizado deve conter entre 4 e 20 caracteres.")
        @Schema(description = "Código alfanumérico customizado opcional (4 a 20 caracteres)", example = "artigo-sdd")
        String code,

        @Schema(description = "Data e hora de expiração opcional em UTC", example = "2026-12-31T23:59:59Z")
        Instant expiresAtUtc
) {
}

