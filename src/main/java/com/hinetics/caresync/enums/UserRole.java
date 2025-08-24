package com.hinetics.caresync.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum UserRole {
    PATIENT,
    CAREGIVER;

    @JsonCreator
    public static UserRole fromString(String value) {
        if (value == null) return PATIENT; // default role if null
        String cleaned = value.replace("-", " ").replace("_", " ").trim();
        return Arrays.stream(UserRole.values())
                .filter(r -> r.name().replace("_", " ").equalsIgnoreCase(cleaned))
                .findFirst()
                .orElse(PATIENT); // default role if unrecognized
    }

    }
