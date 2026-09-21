package com.aether.beauty.api;

import com.aether.beauty.api.exception.UnauthorizedException;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<Map<String, Object>> notFound(EntityNotFoundException ex) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
  public ResponseEntity<Map<String, Object>> badRequest(RuntimeException ex) {
    // These are "expected" failures (bad input, a misconfigured/rejected
    // payment attempt, etc.) rather than bugs, which is why they get a
    // clean 400 instead of a 500 — but "expected" doesn't mean
    // "unimportant to see later": log the full exception, cause chain
    // included, so a real underlying error (like a rejected Paytm
    // request) doesn't disappear without a trace the way this one did.
    log.error("Request rejected: {}", ex.getMessage(), ex);
    return error(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Map<String, Object>> unauthorized(UnauthorizedException ex) {
    return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
    return error(HttpStatus.BAD_REQUEST, "Please fill all required checkout fields correctly.");
  }

  private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
    return ResponseEntity
      .status(status)
      .body(Map.of("timestamp", Instant.now(), "status", status.value(), "error", message));
  }
}
