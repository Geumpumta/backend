package com.gpt.geumpumtabackend.global.jwt.exception;


import com.gpt.geumpumtabackend.global.exception.ExceptionType;

public class JwtNotExistException extends JwtAuthenticationException {
    public JwtNotExistException() {
        super(ExceptionType.JWT_NOT_EXIST);
    }
}
