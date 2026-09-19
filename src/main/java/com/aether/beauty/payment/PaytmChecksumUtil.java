package com.aether.beauty.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Re-implementation of Paytm's official "PaytmChecksum" utility in plain
 * Java. Built from Paytm's own published PHP and Go reference source
 * (SHA-256 hash + AES-128-CBC encryption with a fixed IV) rather than a
 * Maven dependency, since no officially-published Java artifact could be
 * confirmed. The algorithm, as documented and shown in Paytm's own code:
 *
 *   1. hash = SHA-256("<data>|<salt>") as a hex string, salt is 4 random
 *      alphanumeric characters
 *   2. AES-128-CBC encrypt (hash + salt) using the merchant key as the
 *      AES key (must be exactly 16 bytes/characters — Paytm's issued
 *      merchant keys are 16 characters for this reason) and the fixed
 *      IV Paytm's SDKs all use, then base64-encode the result
 *
 * IMPORTANT: this has not been executed against Paytm's servers — it
 * cannot be, from this environment. Before trusting it with a single
 * real transaction, generate a signature with test credentials and
 * confirm Paytm's Initiate Transaction API actually accepts it rather
 * than rejecting the request outright — that's the fastest way to know
 * whether this needs adjustment.
 */
public final class PaytmChecksumUtil {
  private static final String IV = "@@@@&&&&####$$$$";
  private static final String CIPHER_TRANSFORM = "AES/CBC/PKCS5Padding";
  private static final int SALT_LENGTH = 4;
  private static final String SALT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  private PaytmChecksumUtil() {}

  /** Signs a request body (the exact JSON string sent to Paytm) with the merchant key. */
  public static String generateSignature(String data, String merchantKey) {
    try {
      String salt = generateSalt();
      String hashInput = data + "|" + salt;
      String hash = sha256Hex(hashInput);
      return encrypt(hash + salt, merchantKey);
    } catch (Exception ex) {
      throw new IllegalStateException("Could not generate Paytm checksum", ex);
    }
  }

  /** Verifies a signature Paytm sent back (e.g. in a callback) against the data it covers. */
  public static boolean verifySignature(String data, String merchantKey, String checksum) {
    try {
      String decrypted = decrypt(checksum, merchantKey);
      if (decrypted.length() <= SALT_LENGTH) {
        return false;
      }
      String salt = decrypted.substring(decrypted.length() - SALT_LENGTH);
      String hash = decrypted.substring(0, decrypted.length() - SALT_LENGTH);
      String expectedHash = sha256Hex(data + "|" + salt);
      return expectedHash.equalsIgnoreCase(hash);
    } catch (Exception ex) {
      // Any failure (bad base64, wrong key, tampered data) means "not valid" —
      // never let a crypto exception be mistaken for a passing check.
      return false;
    }
  }

  private static String sha256Hex(String input) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
    StringBuilder hex = new StringBuilder();
    for (byte b : hash) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }

  private static String generateSalt() {
    SecureRandom random = new SecureRandom();
    StringBuilder salt = new StringBuilder();
    for (int i = 0; i < SALT_LENGTH; i++) {
      salt.append(SALT_CHARS.charAt(random.nextInt(SALT_CHARS.length())));
    }
    return salt.toString();
  }

  private static String encrypt(String input, String key) throws Exception {
    Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM);
    cipher.init(
      Cipher.ENCRYPT_MODE,
      new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
      new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8))
    );
    return Base64.getEncoder().encodeToString(cipher.doFinal(input.getBytes(StandardCharsets.UTF_8)));
  }

  private static String decrypt(String encryptedBase64, String key) throws Exception {
    Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM);
    cipher.init(
      Cipher.DECRYPT_MODE,
      new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
      new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8))
    );
    return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedBase64)), StandardCharsets.UTF_8);
  }
}
