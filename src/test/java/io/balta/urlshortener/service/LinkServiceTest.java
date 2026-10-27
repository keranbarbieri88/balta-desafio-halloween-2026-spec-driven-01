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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private UrlValidator urlValidator;

    @Mock
    private CustomCodeValidator customCodeValidator;

    @Mock
    private CodeGenerator codeGenerator;

    private LinkService linkService;

    @BeforeEach
    void setUp() {
        linkService = new LinkService(
                linkRepository,
                urlValidator,
                customCodeValidator,
                codeGenerator,
                "http://localhost:8081"
        );
    }

    @Test
    @DisplayName("Deve criar link com codigo alfanumerico gerado automaticamente")
    void shouldCreateLinkWithGeneratedCode() {
        // Arrange
        CreateLinkRequest request = new CreateLinkRequest("https://balta.io", null, null);
        String generatedCode = "abc1234";

        when(codeGenerator.generate()).thenReturn(generatedCode);
        when(linkRepository.existsByCode(generatedCode)).thenReturn(false);
        when(linkRepository.save(any(LinkEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreateLinkResponse response = linkService.createLink(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo(generatedCode);
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8081/" + generatedCode);
        assertThat(response.targetUrl()).isEqualTo("https://balta.io");
        assertThat(response.expiresAtUtc()).isNull();
        verify(urlValidator).validate("https://balta.io");
        verify(linkRepository).save(any(LinkEntity.class));
    }

    @Test
    @DisplayName("Deve criar link com codigo personalizado valido")
    void shouldCreateLinkWithValidCustomCode() {
        // Arrange
        String customCode = "meu-codigo";
        CreateLinkRequest request = new CreateLinkRequest("https://balta.io", customCode, null);

        when(linkRepository.existsByCode(customCode)).thenReturn(false);
        when(linkRepository.save(any(LinkEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreateLinkResponse response = linkService.createLink(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo(customCode);
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8081/" + customCode);
        verify(customCodeValidator).validate(customCode);
        verify(linkRepository).save(any(LinkEntity.class));
    }

    @Test
    @DisplayName("Deve criar link com data futura de expiracao")
    void shouldCreateLinkWithFutureExpirationDate() {
        // Arrange
        Instant future = Instant.now().plus(7, ChronoUnit.DAYS);
        CreateLinkRequest request = new CreateLinkRequest("https://balta.io", "promo-balta", future);

        when(linkRepository.existsByCode("promo-balta")).thenReturn(false);
        when(linkRepository.save(any(LinkEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreateLinkResponse response = linkService.createLink(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.expiresAtUtc()).isEqualTo(future);
        verify(linkRepository).save(any(LinkEntity.class));
    }

    @Test
    @DisplayName("Deve lancar InvalidExpirationDateException se data de expiracao estiver no passado")
    void shouldThrowExceptionWhenExpirationDateIsInThePast() {
        // Arrange
        Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);
        CreateLinkRequest request = new CreateLinkRequest("https://balta.io", "codigo-passado", past);

        // Act & Assert
        assertThatThrownBy(() -> linkService.createLink(request))
                .isInstanceOf(InvalidExpirationDateException.class)
                .hasMessageContaining("deve estar no futuro");

        verify(linkRepository, never()).save(any(LinkEntity.class));
    }

    @Test
    @DisplayName("Deve lancar LinkConflictException se codigo customizado ja estiver em uso")
    void shouldThrowExceptionWhenCustomCodeAlreadyExists() {
        // Arrange
        String duplicateCode = "ja-existe";
        CreateLinkRequest request = new CreateLinkRequest("https://balta.io", duplicateCode, null);

        when(linkRepository.existsByCode(duplicateCode)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> linkService.createLink(request))
                .isInstanceOf(LinkConflictException.class)
                .hasMessageContaining(duplicateCode);

        verify(linkRepository, never()).save(any(LinkEntity.class));
    }

    @Test
    @DisplayName("Deve buscar URL de destino e incrementar contador para link valido")
    void shouldGetTargetUrlAndIncrementCountForValidLink() {
        // Arrange
        String code = "link-ativo";
        LinkEntity link = new LinkEntity(code, "https://balta.io", Instant.now(), null);
        when(linkRepository.findByCode(code)).thenReturn(Optional.of(link));

        // Act
        String targetUrl = linkService.getTargetUrlAndIncrementAccess(code);

        // Assert
        assertThat(targetUrl).isEqualTo("https://balta.io");
        verify(linkRepository).incrementAccessCount(code);
    }

    @Test
    @DisplayName("Deve lancar LinkNotFoundException ao tentar redirecionar codigo inexistente")
    void shouldThrowNotFoundWhenRedirectingNonexistentCode() {
        // Arrange
        String code = "inexistente";
        when(linkRepository.findByCode(code)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> linkService.getTargetUrlAndIncrementAccess(code))
                .isInstanceOf(LinkNotFoundException.class)
                .hasMessageContaining(code);

        verify(linkRepository, never()).incrementAccessCount(any());
    }

    @Test
    @DisplayName("Deve lancar LinkExpiredException ao tentar redirecionar link expirado e nao incrementar contador")
    void shouldThrowExpiredExceptionAndNotIncrementCountForExpiredLink() {
        // Arrange
        String code = "expirado";
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        LinkEntity link = new LinkEntity(code, "https://balta.io", Instant.now().minus(2, ChronoUnit.HOURS), past);
        when(linkRepository.findByCode(code)).thenReturn(Optional.of(link));

        // Act & Assert
        assertThatThrownBy(() -> linkService.getTargetUrlAndIncrementAccess(code))
                .isInstanceOf(LinkExpiredException.class)
                .hasMessageContaining(code);

        verify(linkRepository, never()).incrementAccessCount(any());
    }

    @Test
    @DisplayName("Deve retornar estatisticas publicas corretas para link existente")
    void shouldReturnPublicStatsForExistingLink() {
        // Arrange
        String code = "link-stats";
        Instant created = Instant.now().minus(1, ChronoUnit.DAYS);
        LinkEntity link = new LinkEntity(code, "https://balta.io", created, null);
        link.setAccessCount(50L);
        when(linkRepository.findByCode(code)).thenReturn(Optional.of(link));

        // Act
        LinkStatsResponse stats = linkService.getLinkStats(code);

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.code()).isEqualTo(code);
        assertThat(stats.targetUrl()).isEqualTo("https://balta.io");
        assertThat(stats.accessCount()).isEqualTo(50L);
        assertThat(stats.isExpired()).isFalse();
    }
}

