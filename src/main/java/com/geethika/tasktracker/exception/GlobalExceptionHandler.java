package com.geethika.tasktracker.exception;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.context.request.*;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private ResponseEntity<Object> error(HttpStatusCode status, String message, String path, Map<String, String> fields) {
        var reason = HttpStatus.valueOf(status.value()).getReasonPhrase();
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), reason, message, path, fields));
    }
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Object> notFound(NotFoundException e, HttpServletRequest r) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), r.getRequestURI(), Map.of());
    }
    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Object> conflict(ConflictException e, HttpServletRequest r) {
        return error(HttpStatus.CONFLICT, e.getMessage(), r.getRequestURI(), Map.of());
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> integrity(DataIntegrityViolationException e, HttpServletRequest r) {
        return error(HttpStatus.CONFLICT, "Operation conflicts with an existing unique value or task reference", r.getRequestURI(), Map.of());
    }
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ResponseEntity<Object> badRequest(Exception e, HttpServletRequest r) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), r.getRequestURI(), Map.of());
    }
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return error(status, "Request validation failed", path(request), fields);
    }
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return error(status, "Malformed JSON, unknown field, invalid date or unsupported enum value", path(request), Map.of());
    }
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex instanceof MethodArgumentTypeMismatchException ? "Invalid path or query parameter value" : HttpStatus.valueOf(status.value()).getReasonPhrase();
        return new ResponseEntity<>(new ApiError(Instant.now(), status.value(), HttpStatus.valueOf(status.value()).getReasonPhrase(), message, path(request), Map.of()), headers, status);
    }
    private String path(WebRequest request) { return ((ServletWebRequest) request).getRequest().getRequestURI(); }
}
