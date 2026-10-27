package io.balta.urlshortener.controller;

import io.balta.urlshortener.dto.CreateLinkRequest;
import io.balta.urlshortener.dto.CreateLinkResponse;
import io.balta.urlshortener.dto.LinkStatsResponse;
import io.balta.urlshortener.service.LinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Tag(name = "Links", description = "Endpoints para encurtamento, redirecionamento e estatísticas")
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping("/api/links")
    @Operation(summary = "Criar novo link encurtado", description = "Recebe uma URL longa e gera ou atribui um código curto de redirecionamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Link encurtado com sucesso",
                    content = @Content(schema = @Schema(implementation = CreateLinkResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos ou violação de regras de negócio",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflito: código já em uso",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CreateLinkResponse> createLink(@Valid @RequestBody CreateLinkRequest request) {
        CreateLinkResponse response = linkService.createLink(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create(response.shortUrl()))
                .body(response);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Redirecionar para URL original", description = "Redireciona para o destino via HTTP 302 Found e contabiliza o acesso.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirecionamento executado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Código não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "410", description = "Link expirado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> redirect(@PathVariable("code") String code) {
        String targetUrl = linkService.getTargetUrlAndIncrementAccess(code);
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, targetUrl)
                .build();
    }

    @GetMapping("/api/links/{code}/stats")
    @Operation(summary = "Consultar estatísticas públicas do link", description = "Retorna métricas de acesso sem necessidade de autenticação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métricas retornadas com sucesso",
                    content = @Content(schema = @Schema(implementation = LinkStatsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Código não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LinkStatsResponse> getStats(@PathVariable("code") String code) {
        LinkStatsResponse response = linkService.getLinkStats(code);
        return ResponseEntity.ok(response);
    }
}

