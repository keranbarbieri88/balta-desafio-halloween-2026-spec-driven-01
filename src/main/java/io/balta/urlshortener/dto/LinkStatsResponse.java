package io.balta.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record LinkStatsResponse(
        @Schema(description = "Código identificador do link", example = "artigo-sdd")
        String code,

        @Schema(description = "URL original de destino", example = "https://balta.io/artigos/sdd-com-ia")
        String targetUrl,

        @Schema(description = "Total de acessos e redirecionamentos efetuados", example = "42")
        Long accessCount,

        @Schema(description = "Instante de criação do link em UTC", example = "2026-10-27T10:00:00Z")
        Instant createdAtUtc,

        @Schema(description = "Instante de expiração em UTC (se aplicável)", example = "2026-12-31T23:59:59Z")
        Instant expiresAtUtc,

        @Schema(description = "Indica se o link já se encontra expirado no momento da consulta", example = "false")
        boolean isExpired
) {
}

