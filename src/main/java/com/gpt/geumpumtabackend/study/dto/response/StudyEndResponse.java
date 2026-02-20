package com.gpt.geumpumtabackend.study.dto.response;

import com.gpt.geumpumtabackend.badge.dto.response.NewBadgeResponse;

import java.util.List;

public record StudyEndResponse(
        List<NewBadgeResponse> newBadges
) {
    public static StudyEndResponse of(List<NewBadgeResponse> newBadges) {
        return new StudyEndResponse(newBadges);
    }
}
