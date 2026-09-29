package com.example.certificates.util;

import java.security.SecureRandom;

/**
 * Generates public certificate codes that are non-sequential and safe to put in a
 * public link. Format: CERT-XXXXXXXXXXXX using Crockford base32 (no ambiguous chars).
 * ~60 bits of entropy makes the codes unguessable and collision-safe at scale.
 */
public final class PublicCodeGenerator {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicCodeGenerator() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder("CERT-");
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
