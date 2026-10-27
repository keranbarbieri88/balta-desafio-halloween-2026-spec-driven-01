package io.balta.urlshortener.exception;

public class LinkConflictException extends RuntimeException {

    public LinkConflictException(String code) {
        super("O código '" + code + "' já está em uso no sistema. Escolha outro código.");
    }
}

