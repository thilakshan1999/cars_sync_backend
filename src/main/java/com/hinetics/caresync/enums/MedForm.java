package com.hinetics.caresync.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum MedForm {
    TABLET,
    CAPSULE,
    SYRUP,
    INJECTION,
    POWDER,
    DROPS,
    CREAM,
    GEL,
    SPRAY,
    OTHER;

    @JsonCreator
    public static MedForm fromString(String value) {
        if (value == null) return OTHER;
        String cleaned = value.replace("-", " ").replace("_", " ").trim();
        return Arrays.stream(MedForm.values())
                .filter(f -> f.name().replace("_", " ").equalsIgnoreCase(cleaned))
                .findFirst()
                .orElse(OTHER);
    }
}
