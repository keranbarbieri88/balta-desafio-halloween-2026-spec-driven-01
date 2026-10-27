package io.balta.urlshortener.service;

import io.balta.urlshortener.domain.LinkEntity;
import io.balta.urlshortener.dto.CreateLinkRequest;
import io.balta.urlshortener.dto.CreateLinkResponse;
import io.balta.urlshortener.dto.LinkStatsResponse;
import io.balta.urlshortener.exception.InvalidExpirationDateException;
import io.balta.urlshortener.exception.LinkConflictException;
import io.balta.urlshortener.exception.LinkExpiredException;
import io.balta.urlshortener.exception.LinkNotFoundException;
import io.balta.urlshortener.repository.LinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LinkService {

    private final LinkRepository linkRepository;
    private final UrlValidator urlValidator;
    private final CustomCodeValidator customCodeValidator;
    private final CodeGenerator codeGenerator;
    private final String baseUrl;

    public LinkService(
            LinkRepository linkRepository,
            UrlValidator urlValidator,
            CustomCodeValidator customCodeValidator,
            CodeGenerator codeGenerator,
            @Value("${app.base-url:http://localhost:8081}") String baseUrl
    ) {
        this.linkRepository = linkRepository;
        this.urlValidator = urlValidator;
        this.customCodeValidator = customCodeValidator;
        this.codeGenerator = codeGenerator;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public CreateLinkResponse createLink(CreateLinkRequest request) {
        urlValidator.validate(request.targetUrl());

        String code;
        if (request.code() != null && !request.code().isBlank()) {
            code = request.code().trim();
            customCodeValidator.validate(code);
            if (linkRepository.existsByCode(code)) {
                throw new LinkConflictException(code);
            }
        } else {
            code = generateUniqueCode();
        }

        Instant expiresAtUtc = request.expiresAtUtc();
        if (expiresAtUtc != null && expiresAtUtc.isBefore(Instant.now())) {
            throw new InvalidExpirationDateException("A data e hora de expiração deve estar no futuro.");
        }

        LinkEntity linkEntity = new LinkEntity(code, request.targetUrl().trim(), Instant.now(), expiresAtUtc);

        try {
            LinkEntity saved = linkRepository.save(linkEntity);
            return new CreateLinkResponse(
                    saved.getCode(),
                    buildShortUrl(saved.getCode()),
                    saved.getTargetUrl(),
                    saved.getExpiresAtUtc()
            );
        } catch (DataIntegrityViolationException e) {
            throw new LinkConflictException(code);
        }
    }

    @Transactional
    public String getTargetUrlAndIncrementAccess(String code) {
        LinkEntity link = linkRepository.findByCode(code)
                .orElseThrow(() -> new LinkNotFoundException(code));

        if (link.isExpired(Instant.now())) {
            throw new LinkExpiredException(code);
        }

        linkRepository.incrementAccessCount(code);
        return link.getTargetUrl();
    }

    @Transactional(readOnly = true)
    public LinkStatsResponse getLinkStats(String code) {
        LinkEntity link = linkRepository.findByCode(code)
                .orElseThrow(() -> new LinkNotFoundException(code));

        return new LinkStatsResponse(
                link.getCode(),
                link.getTargetUrl(),
                link.getAccessCount(),
                link.getCreatedAtUtc(),
                link.getExpiresAtUtc(),
                link.isExpired(Instant.now())
        );
    }

    private String generateUniqueCode() {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            String candidate = codeGenerator.generate();
            if (!linkRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código único após múltiplas tentativas.");
    }

    private String buildShortUrl(String code) {
        return baseUrl + "/" + code;
    }
}

