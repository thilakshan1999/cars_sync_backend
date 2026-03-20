package com.hinetics.caresync.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum CareGiverPermission {
    VIEW_ONLY,      // Can only view patient info/documents
    FULL_ACCESS;

    @JsonCreator
    public static CareGiverPermission fromString(String value) {
        if (value == null) return VIEW_ONLY; // default if null
        return Arrays.stream(CareGiverPermission.values())
                .filter(p -> p.name().equalsIgnoreCase(value.trim())
                        || p.name().replace("_", " ").equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(VIEW_ONLY); // fallback default
    }
}
