package com.example.clsc.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class DateTimeUtils {

    private static final Logger logger = LoggerFactory.getLogger(DateTimeUtils.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final ChronoUnit COMPARISON_PRECISION = ChronoUnit.MILLIS;

    public static boolean areTimestampsEqual(LocalDateTime t1, LocalDateTime t2) {
        if (t1 == null || t2 == null) return false;
        return t1.truncatedTo(COMPARISON_PRECISION).isEqual(t2.truncatedTo(COMPARISON_PRECISION));
    }

    public static int compareTimestamps(LocalDateTime t1, LocalDateTime t2) {
        if (t1 == null && t2 == null) return 0;
        if (t1 == null) return -1;
        if (t2 == null) return 1;
        return t1.truncatedTo(COMPARISON_PRECISION).compareTo(t2.truncatedTo(COMPARISON_PRECISION));
    }

    public static LocalDateTime extractTimeStampFromJson(String json, String method) {
        try {
            if (json == null) return null;
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            Object updatedAt = map.get(method);
            if (updatedAt instanceof LocalDateTime) return (LocalDateTime) updatedAt;
            else if (updatedAt instanceof String) return LocalDateTime.parse((String) updatedAt);
        } catch (DateTimeParseException e) {
            logger.warn("Date parsing failed for updatedAt: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("Failed to extract updatedAt: {}", e.getMessage());
        }
        return null;
    }

}
