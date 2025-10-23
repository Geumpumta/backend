package com.gpt.geumpumtabackend.global.jwt.exception;


import com.gpt.geumpumtabackend.global.exception.ExceptionType;

public class JwtAccessDeniedException extends JwtAuthenticationException {
    public JwtAccessDeniedException() {
        super(ExceptionType.ACCESS_DENIED);
    }
}
