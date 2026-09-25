package com.wersiv.datingsite.backend.common.exception;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * All error messages are resolved through MessageSource (messages.properties /
 * messages_ru.properties) using the locale Spring picked from the request's
 * Accept-Language header - see LocaleConfig.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<ErrorResponse> handleLocalized(LocalizedException ex) {
        String message = messageSource.getMessage(
                ex.getCode(), ex.getArgs(), ex.getCode(), LocaleContextHolder.getLocale());
        return build(ex.getStatus(), message);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        String message = messageSource.getMessage(
                "error.auth.invalid_credentials", null, LocaleContextHolder.getLocale());
        return build(HttpStatus.UNAUTHORIZED, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse error = new ErrorResponse(status.value(), message, LocalDateTime.now());
        return ResponseEntity.status(status).body(error);
    }
}
