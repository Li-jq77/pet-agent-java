package com.ljq.petagent.config;

import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

public class DjangoPasswordEncoder implements PasswordEncoder {

    private static final String ALGORITHM = "pbkdf2_sha256";
    private static final int DEFAULT_ITERATIONS = 1_200_000;
    private static final int SALT_BYTES = 12;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String encode(CharSequence rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        String saltText = Base64.getEncoder().withoutPadding().encodeToString(salt);
        String hash = pbkdf2(rawPassword.toString(), saltText, DEFAULT_ITERATIONS);
        return ALGORITHM + "$" + DEFAULT_ITERATIONS + "$" + saltText + "$" + hash;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        String[] parts = encodedPassword.split("\\$", -1);
        if (parts.length != 4 || !ALGORITHM.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            String expected = parts[3];
            String actual = pbkdf2(rawPassword.toString(), parts[2], iterations);
            return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.US_ASCII),
                expected.getBytes(StandardCharsets.US_ASCII)
            );
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String pbkdf2(String password, String salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt.getBytes(StandardCharsets.UTF_8),
                iterations,
                KEY_BITS
            );
            String algorithm = "PBKDF2WithHmacSHA256";
            if (System.getProperty("java.version", "").startsWith("1.8")) {
                algorithm = "PBKDF2WithHmacSHA256";
            }
            byte[] hash = SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成 Django 兼容密码", ex);
        }
    }

    public static boolean looksLikeDjangoHash(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).startsWith("pbkdf2_");
    }
}
