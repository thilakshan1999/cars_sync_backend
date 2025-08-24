package com.hinetics.caresync.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum EntityStatus {
    NEW,
    UPDATED,
    SAME;

    @JsonCreator
    public static EntityStatus fromString(String value) {
        if (value == null) return SAME; // default if null
        String cleaned = value.replace("-", " ").replace("_", " ").trim();
        return Arrays.stream(EntityStatus.values())
                .filter(status -> status.name().replace("_", " ").equalsIgnoreCase(cleaned))
                .findFirst()
                .orElse(SAME); // default fallback
    }

}
