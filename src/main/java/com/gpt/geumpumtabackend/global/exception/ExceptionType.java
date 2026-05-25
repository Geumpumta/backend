package com.gpt.geumpumtabackend.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum ExceptionType {

    // Common
    UNEXPECTED_SERVER_ERROR(INTERNAL_SERVER_ERROR,"C001","예상치 못한 에러가 발생했습니다."),
    BINDING_ERROR(BAD_REQUEST,"C002","바인딩시 에러가 발생했습니다."),
    ESSENTIAL_FIELD_MISSING_ERROR(NO_CONTENT , "C003","필수적인 필드가 부재합니다"),

    // Security
    ILLEGAL_REGISTRATION_ID(NOT_ACCEPTABLE, "S001", "잘못된 registration id 입니다"),
    NEED_AUTHORIZED(UNAUTHORIZED, "S002", "인증이 필요합니다."),
    ACCESS_DENIED(FORBIDDEN, "S003", "권한이 없습니다."),
    JWT_EXPIRED(UNAUTHORIZED, "S004", "JWT 토큰이 만료되었습니다."),
    JWT_INVALID(UNAUTHORIZED, "S005", "JWT 토큰이 올바르지 않습니다."),
    JWT_NOT_EXIST(UNAUTHORIZED, "S006", "JWT 토큰이 존재하지 않습니다."),
    SESSION_INVALID(UNAUTHORIZED, "S007", "로그인 세션이 유효하지 않습니다."),

    // Token
    REFRESH_TOKEN_NOT_EXIST(NOT_FOUND, "T001", "리프래시 토큰이 존재하지 않습니다"),
    TOKEN_NOT_MATCHED(UNAUTHORIZED, "T002","일치하지 않는 토큰입니다"),

    // User
    USER_NOT_FOUND(NOT_FOUND, "U001","사용자가 존재하지 않습니다"),
    SCHOOL_EMAIL_ALREADY_REGISTERED(FORBIDDEN, "U002", "학교 이메일이 등록된 상태입니다"),
    DUPLICATED_SCHOOL_EMAIL(CONFLICT, "U003", "이미 사용중인 학교 이메일입니다"),
    DEPARTMENT_NOT_FOUND(BAD_REQUEST, "U004", "존재하지 않는 학과 명입니다"),
    USER_WITHDRAWN(FORBIDDEN, "U005", "탈퇴한 사용자입니다."),
    DUPLICATED_STUDENT_ID(CONFLICT, "U006", "이미 사용중인 학번입니다."),

    // Mail
    CANT_SEND_MAIL(INTERNAL_SERVER_ERROR, "M001", "인증코드 전송에 실패했습니다."),

    // Study
    STUDY_SESSION_NOT_FOUND(NOT_FOUND,"ST001","해당 공부 세션을 찾을 수 없습니다."),
    ALREADY_STUDY_SESSION(CONFLICT, "ST002", "세션은 하나만 가능합니다."),
    INVALID_END_TIME(CONFLICT,"ST003","유효하지 않은 종료시간입니다."),

    // WiFi
    WIFI_NOT_CAMPUS_NETWORK(FORBIDDEN, "W001", "캠퍼스 네트워크가 아닙니다"),
    WIFI_VALIDATION_ERROR(INTERNAL_SERVER_ERROR, "W002", "Wi-Fi 검증 중 오류가 발생했습니다"),
    WIFI_INVALID_FORMAT(BAD_REQUEST, "W003", "Wi-Fi 정보 형식이 올바르지 않습니다"),

    // Image
    INVALID_IMAGE_FILE(FORBIDDEN, "I001", "허용되지 않은 이미지 형식입니다."),
    IMAGE_SIZE_EXCEEDED(FORBIDDEN, "I002", "이미지 용량이 초과했습니다."),
    IMAGE_UPLOAD_FAILED(INTERNAL_SERVER_ERROR, "I003", "이미지 업로드에 실패했습니다."),


    // board
    BOARD_NOT_FOUND(BAD_REQUEST, "BD001", "존재하지 않는 게시물입니다."),

    // Season
    NO_ACTIVE_SEASON(NOT_FOUND, "SE001", "현재 진행중인 시즌이 없습니다"),
    SEASON_NOT_FOUND(NOT_FOUND, "SE002", "시즌을 찾을 수 없습니다"),
    SEASON_NOT_ENDED(BAD_REQUEST, "SE003", "시즌이 아직 종료되지 않았습니다"),
    SEASON_ALREADY_ENDED(BAD_REQUEST, "SE004", "이미 종료된 시즌입니다"),
    SEASON_INVALID_DATE_RANGE(BAD_REQUEST, "SE005", "시즌 종료일은 시작일보다 이후여야 합니다"),

    // FCM
    FCM_SEND_FAILED(INTERNAL_SERVER_ERROR, "F001", "푸시 알림 전송에 실패했습니다."),
    FCM_INVALID_TOKEN(BAD_REQUEST, "F002", "유효하지 않은 FCM 토큰입니다."),
    FCM_TOKEN_NOT_FOUND(NOT_FOUND, "F003", "등록된 FCM 토큰이 없습니다."),

    // Badge
    BADGE_NOT_FOUND(NOT_FOUND, "B001", "배지가 존재하지 않습니다"),
    BADGE_NOT_OWNED(FORBIDDEN, "B002", "해당 배지를 소유하지 않습니다"),
    BADGE_CODE_ALREADY_EXISTS(CONFLICT, "B003", "이미 존재하는 배지 코드입니다"),
    BADGE_IN_USE(CONFLICT, "B004", "이미 지급되어 삭제할 수 없는 배지입니다"),

    // Maintenance
    MAINTENANCE_IN_PROGRESS(SERVICE_UNAVAILABLE, "MT001", "서버 점검 중입니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
