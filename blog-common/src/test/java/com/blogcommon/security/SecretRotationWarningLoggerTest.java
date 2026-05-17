package com.blogcommon.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretRotationWarningLoggerTest {
    @Test
    void unsafeDefaultsAreDetected() {
        assertTrue(SecretRotationWarningLogger.isUnsafeSecret("change-me"));
        assertTrue(SecretRotationWarningLogger.isUnsafeSecret("change-this-jwt-secret-with-at-least-32-random-bytes"));
        assertTrue(SecretRotationWarningLogger.isUnsafeSecret("blog-cloud-secret-key-blog-cloud-secret-key-blog-cloud-secret-key"));
        assertTrue(SecretRotationWarningLogger.isUnsafeSecret(""));
    }

    @Test
    void generatedLookingSecretIsAllowed() {
        assertFalse(SecretRotationWarningLogger.isUnsafeSecret("qBP1qkqDGycEuqGLxvklSdBHQDDvZnP3wjiiB1QaP8Q="));
    }
}
