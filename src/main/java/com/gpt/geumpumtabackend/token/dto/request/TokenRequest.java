package com.gpt.geumpumtabackend.token.dto.request;

public record TokenRequest (
    String accessToken,
    String refreshToken
){

}
