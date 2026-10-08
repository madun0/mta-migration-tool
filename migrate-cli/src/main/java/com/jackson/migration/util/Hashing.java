package com.jackson.migration.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides deterministic SHA-256 hashing used to detect unexpected working-tree changes around checkpoints.
 */
public final class Hashing {
    private Hashing() {}

    /**
     * Computes a lowercase hexadecimal SHA-256 digest for a UTF-8 string.
     *
     * @param value value to hash
     * @return SHA-256 digest
     */
    public static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
