package io.balta.urlshortener.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            InvalidUrlException.class,
            InvalidCustomCodeException.class,
            InvalidExpirationDateException.class,
            IllegalArgumentException.class
    })
    public ProblemDetail handleBadRequestExceptions(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Erro de validação ou parâmetro inválido");
        problem.setType(URI.create("https://balta.io/erros/bad-request"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        String detailMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        if (detailMessage.isBlank()) {
            detailMessage = "Falha na validação do corpo da requisição.";
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detailMessage);
        problem.setTitle("Erro de validação de payload");
        problem.setType(URI.create("https://balta.io/erros/validacao-payload"));
        return problem;
    }

    @ExceptionHandler(LinkNotFoundException.class)
    public ProblemDetail handleNotFoundException(LinkNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Link não encontrado");
        problem.setType(URI.create("https://balta.io/erros/not-found"));
        return problem;
    }

    @ExceptionHandler({LinkConflictException.class, DataIntegrityViolationException.class})
    public ProblemDetail handleConflictException(Exception ex) {
        String detail = ex instanceof LinkConflictException
                ? ex.getMessage()
                : "Conflito de unicidade: o código informado já está em uso.";

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setTitle("Conflito de código");
        problem.setType(URI.create("https://balta.io/erros/conflito"));
        return problem;
    }

    @ExceptionHandler(LinkExpiredException.class)
    public ProblemDetail handleExpiredException(LinkExpiredException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
        problem.setTitle("Link expirado");
        problem.setType(URI.create("https://balta.io/erros/link-expirado"));
        return problem;
    }
}

