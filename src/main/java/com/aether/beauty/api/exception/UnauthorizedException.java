package com.aether.beauty.api.exception;

// Thrown for a missing/invalid/expired auth token — mapped to HTTP 401 by
// ApiExceptionHandler, distinct from IllegalArgumentException's 400 since
// "you're not signed in" is a different situation from "bad input".
public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(String message) {
    super(message);
  }
}
