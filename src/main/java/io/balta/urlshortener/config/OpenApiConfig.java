package io.balta.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Encurtador de Links - Balta Halloween 2026")
                        .description("API RESTful para encurtamento de URLs longas, redirecionamento via HTTP 302 e métricas de acesso públicas.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Keran Barbieri")
                                .url("https://github.com/keranbarbieri88")));
    }
}

