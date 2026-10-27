package io.balta.urlshortener.exception;

public class LinkNotFoundException extends RuntimeException {

    public LinkNotFoundException(String code) {
        super("Nenhum link encontrado para o código '" + code + "'.");
    }
}

