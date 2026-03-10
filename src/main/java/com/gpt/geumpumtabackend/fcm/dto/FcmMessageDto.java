package com.gpt.geumpumtabackend.fcm.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class FcmMessageDto {
    private String token;
    private String title;
    private String body;
    private String imageUrl;
    private Map<String, String> data;
}
