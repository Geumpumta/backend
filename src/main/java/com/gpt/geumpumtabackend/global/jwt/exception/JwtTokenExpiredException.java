package com.gpt.geumpumtabackend.global.jwt.exception;


import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import lombok.Getter;

@Getter
public class JwtTokenExpiredException extends JwtAuthenticationException {

    public JwtTokenExpiredException(Throwable cause) {
        super(cause, ExceptionType.JWT_EXPIRED);
    }

}
