package io.balta.urlshortener.exception;

public class InvalidExpirationDateException extends RuntimeException {

    public InvalidExpirationDateException(String message) {
        super(message);
    }
}

