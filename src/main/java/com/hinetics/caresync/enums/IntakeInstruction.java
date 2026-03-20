package com.hinetics.caresync.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum IntakeInstruction {
    BEFORE_EAT,
    AFTER_EAT,
    WHILE_EAT,
    DOES_NOT_MATTER;

    @JsonCreator
    public static IntakeInstruction fromString(String value) {
        if (value == null) return DOES_NOT_MATTER;
        String cleaned = value.replace("-", " ").replace("_", " ").trim();
        return Arrays.stream(IntakeInstruction.values())
                .filter(i -> i.name().replace("_", " ").equalsIgnoreCase(cleaned))
                .findFirst()
                .orElse(DOES_NOT_MATTER);
    }
}
