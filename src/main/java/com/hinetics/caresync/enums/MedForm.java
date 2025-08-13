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
        return Arrays.stream(MedForm.values())
                .filter(f -> f.name().equalsIgnoreCase(value.replace(" ", "_")))
                .findFirst()
                .orElse(OTHER);
    }
}
