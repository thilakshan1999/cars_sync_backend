package com.hinetics.caresync.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum DocumentType {
    PRESCRIPTION,
    MEDICAL_REPORT,
    LAB_REPORT,
    DISCHARGE_SUMMARY,
    REFERRAL_LETTER,
    TEST_RESULT,
    OTHER;

    @JsonCreator
    public static DocumentType fromString(String value) {
        return Arrays.stream(DocumentType.values())
                .filter(type -> type.name().replace("_", " ").equalsIgnoreCase(value.replace("-", " ").replace("_", " ")))
                .findFirst()
                .orElse(OTHER); // default fallback
    }
}
