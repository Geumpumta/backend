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

    // Token
    REFRESH_TOKEN_NOT_EXIST(NOT_FOUND, "T001", "리프래시 토큰이 존재하지 않습니다"),
    TOKEN_NOT_MATCHED(UNAUTHORIZED, "T002","일치하지 않는 토큰입니다"),

    // User
    USER_NOT_FOUND(NOT_FOUND, "U001","사용자가 존재하지 않습니다"),
    USER_NOT_FARMER(FORBIDDEN, "U002", "판매자만 가능한 기능입니다."),
    LOCATION_NOT_FOUND(NOT_FOUND, "U003", "저장된 위치를 찾을 수 없습니다."),
    USER_NICKNAME_DUPLICATED(FORBIDDEN, "U004", "중복된 닉네임을 사용할 수 없습니다."),
    BBAT_NOT_UPDATED(UNAUTHORIZED, "U005", "판매자 밭 정보가 입력되지 않았습니다."),

    // Chat
    CANT_CREATE_CHATROOM(FORBIDDEN, "CH001", "채팅방을 생성할 수 없습니다."),
    CHATROOM_NOT_EXIST(NOT_FOUND, "CH002", "채팅방이 존재하지 않습니다."),

    // WebSocket
    WS_TOKEN_MISSING(UNAUTHORIZED, "WS001", "인증 토큰이 없습니다"),
    WS_TOKEN_INVALID(UNAUTHORIZED, "WS002", "JWT 토큰이 올바르지 않습니다."),
    WS_ROOM_ACCESS_DENIED(FORBIDDEN, "WS003", "채팅방에 대한 접근 권한이 없습니다"),
    WS_INVALID_ROOM_PATH(BAD_REQUEST, "WS004", "잘못된 채팅방 경로입니다"),

    // Image
    IMAGE_ACCESS_DENIED(FORBIDDEN, "I001", "해당 이미지에 대한 권한이 없습니다."),
    IMAGE_UPLOAD_FAILED(INTERNAL_SERVER_ERROR, "I002", "이미지 업로드에 실패했습니다."),
    IMAGE_DELETE_FAILED(INTERNAL_SERVER_ERROR, "I003", "이미지 삭제에 실패했습니다."),
    INVALID_IMAGE_FILE(FORBIDDEN, "I004", "허용되지 않은 이미지입니다."),
    IMAGE_SIZE_EXCEEDED(FORBIDDEN, "I005", "이미지 용량이 초과했습니다."),

    // Review
    REVIEW_NOT_FOUND(NOT_FOUND, "R001", "리뷰가 존재하지 않습니다."),
    CANT_REVIEW_MYSELF(FORBIDDEN, "R002", "자신에게 리뷰를 작성할 수 없습니다."),
    CANT_DELETE_REVIEW(FORBIDDEN, "R003", "다른 사용자의 리뷰를 삭제할 수 없습니다."),
    CANT_REVIEW_FAKE(FORBIDDEN, "R004", "거래 완료 전에 리뷰를 작성할 수 없습니다."),
    CANT_REVIEW_MORE(FORBIDDEN, "R005", "한 상품에 2개 이상의 리뷰를 작성할 수 없습니다."),

    // Product
    INVALID_INPUT(BAD_REQUEST, "P001", "입력 값이 올바르지 않습니다."),
    AI_GENERATION_FAILED(INTERNAL_SERVER_ERROR, "P002", "AI 생성에 실패했습니다."),
    PRODUCT_NOT_FOUND(NOT_FOUND, "P003", "상품이 존재하지 않습니다."),
    CANT_DELETE_PRODUCT(FORBIDDEN, "P004", "다른 사용자의 상품을 삭제할 수 없습니다."),
    CANT_UPDATE_PRODUCT(FORBIDDEN, "P005", "상품 소유자가 아니거나 이미 판매 완료된 상품입니다."),
    PRODUCT_COMPLETE_CONFLICT(CONFLICT, "P006", "동시 처리 충돌로 상품 완료 실패"),
    PRODUCT_COMPLETE_INTERRUPTED(INTERNAL_SERVER_ERROR, "P007", "재시도 중 인터럽트 발생"),
    CATEGORY_NOT_FOUND(NOT_FOUND, "P007", "해당 카테고리가 존재하지 않습니다."),
    CANT_COMPLETE_TRADE(FORBIDDEN, "P008", "거래 완료를 할 수 없습니다."),


    // Zzim
    ALREADY_ZZIMED(FORBIDDEN, "Z001", "이미 찜한 상품입니다."),
    ZZIM_NOT_FOUND(NOT_FOUND, "Z002","존재하지 않는 찜입니다."),
    ZZIM_ACCESS_DENIED(FORBIDDEN, "Z003","해당 찜에 접근할 수 없습니다"),

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
