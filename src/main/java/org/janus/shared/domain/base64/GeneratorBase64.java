package org.janus.shared.domain.base64;

import java.security.SecureRandom;
import java.util.Base64;

public class GeneratorBase64 {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    public static String generateRandomBase64Token() {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        return BASE64_URL_ENCODER.encodeToString(randomBytes);
    }
}
