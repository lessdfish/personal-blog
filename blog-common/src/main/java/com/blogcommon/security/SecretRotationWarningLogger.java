package com.blogcommon.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

@Component
public class SecretRotationWarningLogger implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SecretRotationWarningLogger.class);
    private static final Set<String> UNSAFE_EXACT_VALUES = Set.of(
            "change-me",
            "change-root-password",
            "change-app-password",
            "change-rabbitmq-password",
            "change-this-to-a-long-random-string-at-least-32-chars",
            "change-this-jwt-secret-with-at-least-32-random-bytes",
            "blog-cloud-secret-key-blog-cloud-secret-key-blog-cloud-secret-key"
    );

    private final Environment environment;

    public SecretRotationWarningLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        warnIfUnsafe("auth.jwt.secret", "JWT_SECRET");
        warnIfUnsafe("spring.datasource.password", "MYSQL_PASSWORD");
        warnIfUnsafe("spring.rabbitmq.password", "RABBITMQ_PASSWORD");
    }

    private void warnIfUnsafe(String propertyName, String envName) {
        String value = environment.getProperty(propertyName);
        if (isUnsafeSecret(value)) {
            log.warn("Secret rotation required: {} is unset or uses an unsafe default. Override {} in production.", propertyName, envName);
        }
    }

    static boolean isUnsafeSecret(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return UNSAFE_EXACT_VALUES.contains(normalized)
                || normalized.startsWith("change-")
                || normalized.contains("change-this")
                || normalized.contains("secret-key-blog-cloud");
    }
}
