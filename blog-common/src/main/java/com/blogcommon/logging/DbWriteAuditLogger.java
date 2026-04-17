package com.blogcommon.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class DbWriteAuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("DB_WRITE_AUDIT");

    private DbWriteAuditLogger() {
    }

    public static void logInsert(String table, Object payload) {
        LOGGER.info("INSERT table={} payload={}", table, toSafeString(payload));
    }

    private static String toSafeString(Object payload) {
        if (payload == null) {
            return "null";
        }
        if (payload instanceof CharSequence || payload instanceof Number || payload instanceof Boolean || payload instanceof Enum<?> || payload instanceof Temporal) {
            return String.valueOf(payload);
        }
        if (payload instanceof Map<?, ?> map) {
            Map<String, String> rendered = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                rendered.put(String.valueOf(entry.getKey()), sanitizeValue(String.valueOf(entry.getKey()), entry.getValue()));
            }
            return rendered.toString();
        }
        if (payload instanceof Collection<?> collection) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : collection) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(toSafeString(item));
                first = false;
            }
            builder.append("]");
            return builder.toString();
        }
        if (payload.getClass().isArray()) {
            StringBuilder builder = new StringBuilder("[");
            int length = Array.getLength(payload);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(toSafeString(Array.get(payload, i)));
            }
            builder.append("]");
            return builder.toString();
        }

        Map<String, String> fields = new LinkedHashMap<>();
        Class<?> current = payload.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    fields.putIfAbsent(field.getName(), sanitizeValue(field.getName(), field.get(payload)));
                } catch (IllegalAccessException ignored) {
                    fields.putIfAbsent(field.getName(), "<inaccessible>");
                }
            }
            current = current.getSuperclass();
        }
        return fields.toString();
    }

    private static String sanitizeValue(String fieldName, Object value) {
        if (isSensitive(fieldName)) {
            return "***";
        }
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean || value instanceof Enum<?> || value instanceof Temporal) {
            return String.valueOf(value);
        }
        return toSafeString(value);
    }

    private static boolean isSensitive(String fieldName) {
        String normalized = fieldName == null ? "" : fieldName.toLowerCase(Locale.ROOT);
        return normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("credential");
    }
}
