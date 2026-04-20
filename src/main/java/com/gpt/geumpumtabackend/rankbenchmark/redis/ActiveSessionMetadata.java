package com.gpt.geumpumtabackend.rankbenchmark.redis;

public record ActiveSessionMetadata(
        long sessionId,
        long startAtEpochMilli,
        String department
) {
    public static final String FIELD_SESSION_ID = "sessionId";
    public static final String FIELD_START_AT = "startAt";
    public static final String FIELD_DEPARTMENT = "department";
}
