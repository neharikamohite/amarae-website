package com.aether.beauty.auth;

import com.aether.beauty.api.exception.UnauthorizedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private static final int TOKEN_VALID_DAYS = 30;

  private final UserRepository userRepository;
  private final AuthTokenRepository authTokenRepository;
  private final PasswordHasher passwordHasher;

  public AuthService(UserRepository userRepository, AuthTokenRepository authTokenRepository, PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.authTokenRepository = authTokenRepository;
    this.passwordHasher = passwordHasher;
  }

  @Transactional
  public AuthToken signup(String name, String rawEmail, String rawPassword) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Please enter your name.");
    }
    String email = normalizeEmail(rawEmail);
    if (email.isBlank()) {
      throw new IllegalArgumentException("Please enter a valid email address.");
    }
    if (rawPassword == null || rawPassword.length() < 8) {
      throw new IllegalArgumentException("Password must be at least 8 characters.");
    }
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new IllegalArgumentException("An account with this email already exists — try logging in instead.");
    }

    String salt = passwordHasher.newSalt();
    User user = new User();
    user.setName(name.trim());
    user.setEmail(email);
    user.setPasswordSalt(salt);
    user.setPasswordHash(passwordHasher.hash(rawPassword, salt));
    userRepository.save(user);

    return issueToken(user);
  }

  @Transactional
  public AuthToken login(String rawEmail, String rawPassword) {
    String email = normalizeEmail(rawEmail);
    User user = userRepository
      .findByEmailIgnoreCase(email)
      .orElseThrow(() -> new UnauthorizedException("Incorrect email or password."));

    if (rawPassword == null || !passwordHasher.matches(rawPassword, user.getPasswordSalt(), user.getPasswordHash())) {
      throw new UnauthorizedException("Incorrect email or password.");
    }
    return issueToken(user);
  }

  @Transactional
  public void logout(String token) {
    if (token != null && !token.isBlank()) {
      authTokenRepository.deleteByToken(token);
    }
  }

  /**
   * Resolves a bearer token to its User, or throws. Used by any endpoint
   * that requires the customer to be signed in.
   */
  public User requireUser(String token) {
    if (token == null || token.isBlank()) {
      throw new UnauthorizedException("Please sign in.");
    }
    AuthToken authToken = authTokenRepository
      .findByToken(token)
      .orElseThrow(() -> new UnauthorizedException("Your session has expired — please sign in again."));
    if (authToken.getExpiresAt() != null && authToken.getExpiresAt().isBefore(Instant.now())) {
      authTokenRepository.delete(authToken);
      throw new UnauthorizedException("Your session has expired — please sign in again.");
    }
    return authToken.getUser();
  }

  /**
   * Same as requireUser, but returns null instead of throwing — for
   * endpoints like checkout where being signed in is optional and just
   * links the order to the account when it's present.
   */
  public User resolveUserOrNull(String token) {
    try {
      return requireUser(token);
    } catch (UnauthorizedException ex) {
      return null;
    }
  }

  private AuthToken issueToken(User user) {
    AuthToken authToken = new AuthToken();
    authToken.setToken(UUID.randomUUID().toString());
    authToken.setUser(user);
    authToken.setExpiresAt(Instant.now().plus(TOKEN_VALID_DAYS, ChronoUnit.DAYS));
    return authTokenRepository.save(authToken);
  }

  private String normalizeEmail(String rawEmail) {
    return rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
  }
}
