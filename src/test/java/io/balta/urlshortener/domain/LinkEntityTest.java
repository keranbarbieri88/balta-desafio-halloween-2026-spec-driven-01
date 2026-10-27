package io.balta.urlshortener.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LinkEntityTest {

    @Test
    @DisplayName("Nao deve estar expirado quando a data de expiracao for nula (link eterno)")
    void shouldNotBeExpiredWhenExpiresAtUtcIsNull() {
        // Arrange
        LinkEntity link = new LinkEntity("abc1234", "https://balta.io", Instant.now(), null);

        // Act
        boolean expired = link.isExpired(Instant.now());

        // Assert
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("Nao deve estar expirado quando o instante de referencia for anterior a data de expiracao")
    void shouldNotBeExpiredWhenBeforeExpirationDate() {
        // Arrange
        Instant future = Instant.now().plus(2, ChronoUnit.HOURS);
        LinkEntity link = new LinkEntity("abc1234", "https://balta.io", Instant.now(), future);

        // Act
        boolean expired = link.isExpired(Instant.now());

        // Assert
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("Deve estar expirado quando o instante de referencia for posterior a data de expiracao")
    void shouldBeExpiredWhenPastExpirationDate() {
        // Arrange
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        LinkEntity link = new LinkEntity("abc1234", "https://balta.io", Instant.now().minus(3, ChronoUnit.HOURS), past);

        // Act
        boolean expired = link.isExpired(Instant.now());

        // Assert
        assertThat(expired).isTrue();
    }
}

