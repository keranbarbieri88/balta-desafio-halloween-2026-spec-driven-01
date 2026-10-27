package io.balta.urlshortener.exception;

public class InvalidCustomCodeException extends RuntimeException {

    public InvalidCustomCodeException(String message) {
        super(message);
    }
}

