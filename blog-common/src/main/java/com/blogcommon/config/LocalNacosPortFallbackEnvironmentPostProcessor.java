package com.blogcommon.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * On local Windows setups, 9848 is often unavailable because 8848/9848 are used by
 * previous Nacos mappings or excluded port ranges. If the app resolves Nacos to 8848
 * but local 8948/9948 are the actual reachable ports, override the address early.
 */
public class LocalNacosPortFallbackEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String OVERRIDE_SOURCE_NAME = "localNacosPortFallback";
    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String DISCOVERY_KEY = "spring.cloud.nacos.discovery.server-addr";
    private static final String CONFIG_KEY = "spring.cloud.nacos.config.server-addr";
    private static final String LEGACY_KEY = "spring.cloud.nacos.server-addr";
    private static final String ENV_KEY = "NACOS_SERVER_ADDR";
    private static final String DOT_ENV_SOURCE_NAME = "rootDotEnvNacosOverride";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dotEnvNacos = loadRootDotEnvValue(ENV_KEY);
        if (dotEnvNacos != null && !dotEnvNacos.isBlank()) {
            Map<String, Object> dotEnvOverrides = new LinkedHashMap<>();
            dotEnvOverrides.put(DISCOVERY_KEY, dotEnvNacos);
            dotEnvOverrides.put(CONFIG_KEY, dotEnvNacos);
            dotEnvOverrides.put(LEGACY_KEY, dotEnvNacos);
            dotEnvOverrides.put(ENV_KEY, dotEnvNacos);
            environment.getPropertySources().addFirst(new MapPropertySource(DOT_ENV_SOURCE_NAME, dotEnvOverrides));
            return;
        }

        String resolved = firstNonBlank(
                environment.getProperty(DISCOVERY_KEY),
                environment.getProperty(CONFIG_KEY),
                environment.getProperty(LEGACY_KEY),
                environment.getProperty(ENV_KEY)
        );

        if (!shouldUseLocalFallback(resolved)) {
            return;
        }

        if (!isReachable(LOCAL_HOST, 8948) || !isReachable(LOCAL_HOST, 9948)) {
            return;
        }

        if (isReachable(LOCAL_HOST, 8848) && isReachable(LOCAL_HOST, 9848)) {
            return;
        }

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put(DISCOVERY_KEY, LOCAL_HOST + ":8948");
        overrides.put(CONFIG_KEY, LOCAL_HOST + ":8948");
        overrides.put(LEGACY_KEY, LOCAL_HOST + ":8948");
        overrides.put(ENV_KEY, LOCAL_HOST + ":8948");
        environment.getPropertySources().addFirst(new MapPropertySource(OVERRIDE_SOURCE_NAME, overrides));
    }

    private static String loadRootDotEnvValue(String key) {
        Path current = Paths.get("").toAbsolutePath().normalize();
        for (Path cursor = current; cursor != null; cursor = cursor.getParent()) {
            Path envFile = cursor.resolve(".env");
            if (!Files.isRegularFile(envFile)) {
                continue;
            }

            try (BufferedReader reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    int separator = trimmed.indexOf('=');
                    if (separator <= 0) {
                        continue;
                    }

                    String currentKey = trimmed.substring(0, separator).trim();
                    if (!key.equals(currentKey)) {
                        continue;
                    }

                    String value = trimmed.substring(separator + 1).trim();
                    return value.isBlank() ? null : value;
                }
            } catch (IOException ignored) {
                return null;
            }
        }

        return null;
    }

    private static boolean shouldUseLocalFallback(String resolved) {
        if (resolved == null || resolved.isBlank()) {
            return true;
        }

        return resolved.equals("127.0.0.1:8848")
                || resolved.equals("localhost:8848")
                || resolved.equals("http://127.0.0.1:8848")
                || resolved.equals("http://localhost:8848");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isReachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), (int) Duration.ofMillis(300).toMillis());
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
