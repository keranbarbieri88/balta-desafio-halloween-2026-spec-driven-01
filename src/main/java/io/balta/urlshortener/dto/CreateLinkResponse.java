package io.balta.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record CreateLinkResponse(
        @Schema(description = "Código identificador do link encurtado", example = "artigo-sdd")
        String code,

        @Schema(description = "URL completa encurtada para redirecionamento", example = "http://localhost:8081/artigo-sdd")
        String shortUrl,

        @Schema(description = "URL original de destino", example = "https://balta.io/artigos/sdd-com-ia")
        String targetUrl,

        @Schema(description = "Data e hora limite de validade do link em UTC", example = "2026-12-31T23:59:59Z")
        Instant expiresAtUtc
) {
}

