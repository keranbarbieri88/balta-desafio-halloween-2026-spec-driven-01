package io.balta.urlshortener.service;

import io.balta.urlshortener.exception.InvalidUrlException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UrlValidator {

    private static final int MAX_URL_LENGTH = 2048;
    private final Set<String> blockedHosts;

    public UrlValidator(@Value("${app.blocked-hosts:localhost,127.0.0.1,::1,0.0.0.0}") String blockedHostsConfig) {
        this.blockedHosts = Arrays.stream(blockedHostsConfig.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public void validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidUrlException("A URL de destino é obrigatória e não pode ser vazia.");
        }

        String trimmedUrl = rawUrl.trim();

        if (trimmedUrl.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("A URL de destino excede o limite máximo permitido de " + MAX_URL_LENGTH + " caracteres.");
        }

        URI uri;
        try {
            uri = URI.create(trimmedUrl);
        } catch (IllegalArgumentException e) {
            throw new InvalidUrlException("A URL fornecida possui formato inválido.");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("A URL de destino deve utilizar estritamente o protocolo HTTP ou HTTPS.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidUrlException("A URL fornecida deve possuir um host válido.");
        }

        String normalizedHost = host.toLowerCase();
        if (blockedHosts.contains(normalizedHost)) {
            throw new InvalidUrlException("Não é permitido encurtar URLs que apontam para o próprio domínio da aplicação (prevenção de loop).");
        }
    }

    public Set<String> getBlockedHosts() {
        return new HashSet<>(blockedHosts);
    }
}

