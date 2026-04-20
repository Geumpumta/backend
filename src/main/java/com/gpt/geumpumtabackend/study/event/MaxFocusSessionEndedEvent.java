package com.gpt.geumpumtabackend.study.event;

public record MaxFocusSessionEndedEvent(Long userId, int maxFocusHours) {
}
