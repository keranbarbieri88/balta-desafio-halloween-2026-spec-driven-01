package io.balta.urlshortener.exception;

public class LinkExpiredException extends RuntimeException {

    public LinkExpiredException(String code) {
        super("O link associado ao código '" + code + "' expirou e não está mais disponível.");
    }
}

