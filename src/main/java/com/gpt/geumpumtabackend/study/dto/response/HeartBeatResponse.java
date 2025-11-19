package com.gpt.geumpumtabackend.study.dto.response;

import com.gpt.geumpumtabackend.study.domain.SessionStatus;

public record HeartBeatResponse(SessionStatus sessionStatus) {
}
