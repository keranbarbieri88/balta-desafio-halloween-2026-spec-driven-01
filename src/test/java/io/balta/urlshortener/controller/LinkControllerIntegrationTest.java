package io.balta.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.balta.urlshortener.dto.CreateLinkRequest;
import io.balta.urlshortener.dto.CreateLinkResponse;
import io.balta.urlshortener.dto.LinkStatsResponse;
import io.balta.urlshortener.exception.GlobalExceptionHandler;
import io.balta.urlshortener.exception.InvalidUrlException;
import io.balta.urlshortener.exception.LinkConflictException;
import io.balta.urlshortener.exception.LinkExpiredException;
import io.balta.urlshortener.exception.LinkNotFoundException;
import io.balta.urlshortener.service.LinkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LinkController.class)
@Import(GlobalExceptionHandler.class)
class LinkControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LinkService linkService;

    // --- T-10: POST /api/links (Caminho Feliz e Triste) ---

    @Test
    @DisplayName("POST /api/links - Deve retornar 201 Created quando o payload for valido")
    void shouldReturn201CreatedWhenValidPayload() throws Exception {
        // Arrange
        CreateLinkRequest request = new CreateLinkRequest("https://balta.io", "artigo-sdd", null);
        CreateLinkResponse expectedResponse = new CreateLinkResponse(
                "artigo-sdd",
                "http://localhost:8081/artigo-sdd",
                "https://balta.io",
                null
        );

        when(linkService.createLink(any(CreateLinkRequest.class))).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost:8081/artigo-sdd"))
                .andExpect(jsonPath("$.code").value("artigo-sdd"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8081/artigo-sdd"))
                .andExpect(jsonPath("$.targetUrl").value("https://balta.io"));
    }

    @Test
    @DisplayName("POST /api/links - Deve retornar 400 Bad Request com RFC 7807 quando o payload for invalido")
    void shouldReturn400BadRequestWhenPayloadIsInvalid() throws Exception {
        // Arrange
        CreateLinkRequest request = new CreateLinkRequest("", "abc", null); // URL em branco e código curto (< 4)

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("POST /api/links - Deve retornar 400 Bad Request quando UrlValidator lanca InvalidUrlException")
    void shouldReturn400BadRequestWhenUrlValidatorFails() throws Exception {
        // Arrange
        CreateLinkRequest request = new CreateLinkRequest("http://localhost:8081/loop", null, null);
        when(linkService.createLink(any(CreateLinkRequest.class)))
                .thenThrow(new InvalidUrlException("Não é permitido encurtar URLs que apontam para o próprio domínio."));

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Não é permitido encurtar URLs que apontam para o próprio domínio."));
    }

    @Test
    @DisplayName("POST /api/links - Deve retornar 409 Conflict com RFC 7807 quando codigo ja estiver em uso")
    void shouldReturn409ConflictWhenCodeAlreadyExists() throws Exception {
        // Arrange
        CreateLinkRequest request = new CreateLinkRequest("https://balta.io", "link-duplicado", null);
        when(linkService.createLink(any(CreateLinkRequest.class)))
                .thenThrow(new LinkConflictException("link-duplicado"));

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflito de código"))
                .andExpect(jsonPath("$.detail").value("O código 'link-duplicado' já está em uso no sistema. Escolha outro código."));
    }

    // --- T-12: GET /{code} (Redirecionamento HTTP 302, 404 e 410) ---

    @Test
    @DisplayName("GET /{code} - Deve redirecionar com status HTTP 302 Found e cabecalho Location")
    void shouldRedirectWithHttp302FoundAndLocationHeader() throws Exception {
        // Arrange
        String code = "balta-promo";
        String targetUrl = "https://balta.io/promocao";
        when(linkService.getTargetUrlAndIncrementAccess(code)).thenReturn(targetUrl);

        // Act & Assert
        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", targetUrl));
    }

    @Test
    @DisplayName("GET /{code} - Deve retornar 404 Not Found com RFC 7807 quando codigo nao for encontrado")
    void shouldReturn404NotFoundWhenCodeDoesNotExist() throws Exception {
        // Arrange
        String code = "nao-existe";
        when(linkService.getTargetUrlAndIncrementAccess(code))
                .thenThrow(new LinkNotFoundException(code));

        // Act & Assert
        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Link não encontrado"))
                .andExpect(jsonPath("$.detail").value("Nenhum link encontrado para o código 'nao-existe'."));
    }

    @Test
    @DisplayName("GET /{code} - Deve retornar 410 Gone com RFC 7807 quando link estiver expirado")
    void shouldReturn410GoneWhenLinkIsExpired() throws Exception {
        // Arrange
        String code = "link-expirado";
        when(linkService.getTargetUrlAndIncrementAccess(code))
                .thenThrow(new LinkExpiredException(code));

        // Act & Assert
        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.title").value("Link expirado"))
                .andExpect(jsonPath("$.detail").value("O link associado ao código 'link-expirado' expirou e não está mais disponível."));
    }

    // --- T-13: GET /api/links/{code}/stats (Estatísticas Públicas 200 e 404) ---

    @Test
    @DisplayName("GET /api/links/{code}/stats - Deve retornar 200 OK com estatisticas publicas")
    void shouldReturn200OkWithPublicLinkStats() throws Exception {
        // Arrange
        String code = "artigo-sdd";
        Instant createdAt = Instant.parse("2026-10-27T10:00:00Z");
        LinkStatsResponse response = new LinkStatsResponse(
                code,
                "https://balta.io",
                128L,
                createdAt,
                null,
                false
        );

        when(linkService.getLinkStats(code)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/links/{code}/stats", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.targetUrl").value("https://balta.io"))
                .andExpect(jsonPath("$.accessCount").value(128))
                .andExpect(jsonPath("$.createdAtUtc").value("2026-10-27T10:00:00Z"))
                .andExpect(jsonPath("$.isExpired").value(false));
    }

    @Test
    @DisplayName("GET /api/links/{code}/stats - Deve retornar 404 Not Found quando codigo nao existir")
    void shouldReturn404NotFoundWhenStatsCodeDoesNotExist() throws Exception {
        // Arrange
        String code = "codigo-inexistente";
        when(linkService.getLinkStats(code)).thenThrow(new LinkNotFoundException(code));

        // Act & Assert
        mockMvc.perform(get("/api/links/{code}/stats", code))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Link não encontrado"))
                .andExpect(jsonPath("$.detail").value("Nenhum link encontrado para o código 'codigo-inexistente'."));
    }
}

