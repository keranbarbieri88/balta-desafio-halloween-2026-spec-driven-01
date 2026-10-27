package io.balta.urlshortener.controller;

import io.balta.urlshortener.config.OpenApiConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
class SwaggerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSource dataSource;

    @Test
    @DisplayName("Deve permitir acesso com sucesso a documentacao da API no endpoint /v3/api-docs")
    void shouldAccessApiDocsSuccessfully() throws Exception {
        // Arrange
        String apiDocsUrl = "/v3/api-docs";

        // Act & Assert
        mockMvc.perform(get(apiDocsUrl))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve permitir acesso ou redirecionamento para o Swagger UI no endpoint /swagger-ui.html")
    void shouldAccessSwaggerUiSuccessfully() throws Exception {
        // Arrange
        String swaggerUiUrl = "/swagger-ui.html";

        // Act & Assert
        mockMvc.perform(get(swaggerUiUrl))
                .andExpect(status().is3xxRedirection());
    }
}

