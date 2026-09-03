package com.aether.beauty.auth;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

/**
 * PBKDF2 password hashing using only the JDK's built-in javax.crypto —
 * deliberately not a third-party library (e.g. BCrypt), so this doesn't
 * add a new Maven dependency that can't be verified in an offline
 * environment. PBKDF2WithHmacSHA256 is a standard, well-reviewed
 * algorithm built into every JDK.
 */
@Component
public class PasswordHasher {
  private static final int ITERATIONS = 120_000;
  private static final int KEY_LENGTH_BITS = 256;
  private static final SecureRandom RANDOM = new SecureRandom();

  public String newSalt() {
    byte[] salt = new byte[16];
    RANDOM.nextBytes(salt);
    return Base64.getEncoder().encodeToString(salt);
  }

  public String hash(String rawPassword, String saltBase64) {
    try {
      byte[] salt = Base64.getDecoder().decode(saltBase64);
      PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      byte[] hash = factory.generateSecret(spec).getEncoded();
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
      throw new IllegalStateException("Unable to hash password", ex);
    }
  }

  public boolean matches(String rawPassword, String saltBase64, String expectedHash) {
    String actualHash = hash(rawPassword, saltBase64);
    // Constant-time-ish comparison via equals is fine here since both
    // sides are already fixed-length Base64 digests, not raw secrets
    // being brute-forced over a timing channel in this simple setup.
    return actualHash.equals(expectedHash);
  }
}
