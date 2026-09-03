package com.aether.beauty.admin;

import com.aether.beauty.api.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * There's one admin (the store owner), so this is a single shared
 * password from an env var rather than a full per-user account system —
 * same "off until configured" idea as Razorpay/Cloudinary/SMTP: with no
 * ADMIN_PASSWORD set, login always fails rather than defaulting open.
 */
@Service
public class AdminAuthService {
  private static final int TOKEN_VALID_HOURS = 12;

  private final AdminSessionRepository adminSessionRepository;
  private final String adminPassword;

  public AdminAuthService(
    AdminSessionRepository adminSessionRepository,
    @Value("${aether.admin.password:}") String adminPassword
  ) {
    this.adminSessionRepository = adminSessionRepository;
    this.adminPassword = adminPassword;
  }

  @Transactional
  public String login(String suppliedPassword) {
    if (adminPassword == null || adminPassword.isBlank()) {
      throw new IllegalStateException("Admin access hasn't been configured yet — set ADMIN_PASSWORD to enable it.");
    }
    if (suppliedPassword == null || !constantTimeEquals(suppliedPassword, adminPassword)) {
      throw new UnauthorizedException("Incorrect admin password.");
    }

    AdminSession session = new AdminSession();
    session.setToken(UUID.randomUUID().toString());
    session.setExpiresAt(Instant.now().plus(TOKEN_VALID_HOURS, ChronoUnit.HOURS));
    return adminSessionRepository.save(session).getToken();
  }

  @Transactional
  public void logout(String token) {
    if (token != null && !token.isBlank()) {
      adminSessionRepository.deleteByToken(token);
    }
  }

  /**
   * Throws UnauthorizedException unless the token belongs to a live,
   * unexpired admin session. Called at the top of every admin endpoint.
   */
  @Transactional
  public void requireAdmin(String token) {
    if (token == null || token.isBlank()) {
      throw new UnauthorizedException("Admin sign-in required.");
    }
    AdminSession session = adminSessionRepository
      .findByToken(token)
      .orElseThrow(() -> new UnauthorizedException("Admin session expired — please sign in again."));
    if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(Instant.now())) {
      adminSessionRepository.delete(session);
      throw new UnauthorizedException("Admin session expired — please sign in again.");
    }
  }

  private boolean constantTimeEquals(String a, String b) {
    return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }
}
