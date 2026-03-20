package com.hinetics.caresync.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum AppointmentType {
    CONSULTATION,
    FOLLOW_UP,
    SURGERY,
    DIAGNOSIS,
    CHECKUP,
    EMERGENCY,
    OTHER;

    @JsonCreator
    public static AppointmentType fromString(String value) {
        return Arrays.stream(AppointmentType.values())
                .filter(type -> type.name().replace("_", " ").equalsIgnoreCase(value.replace("-", " ").replace("_", " ")))
                .findFirst()
                .orElse(OTHER); // fallback
    }
}
