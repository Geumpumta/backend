# 예외 처리 가이드

---

## ExceptionType 접두사 규칙

| 접두사 | 도메인 | 예시 |
|--------|--------|------|
| `C` | 공통 | `C001`, `C002`, `C003` |
| `S` | 보안 | `S001` ~ `S006` |
| `T` | 토큰 | `T001`, `T002` |
| `U` | 사용자 | `U001` ~ `U006` |
| `M` | 메일 | `M001` |
| `ST` | 학습 | `ST001` ~ `ST003` |
| `W` | WiFi | `W001` ~ `W003` |
| `I` | 이미지 | `I001` ~ `I003` |
| `B` | 게시판 | `B001` |
| `SE` | 시즌 | `SE001` ~ `SE005` |
| `F` | FCM | `F001` ~ `F003` |

새 도메인이면 새 접두사를 정의하고, 기존 도메인이면 마지막 번호 다음을 사용한다.

---

## 에러 코드 추가 방법

`ExceptionType.java`에 enum 상수를 추가한다:

```java
// ExceptionType.java — 해당 도메인 섹션 하단에 추가
EXAMPLE_NOT_FOUND(NOT_FOUND, "EX001", "예시를 찾을 수 없습니다"),
```

각 상수는 `(HttpStatus, 코드문자열, 메시지)` 형태.

---

## 예외 던지기

```java
throw new BusinessException(ExceptionType.EXAMPLE_NOT_FOUND);
```

`GlobalExceptionHandler`가 `BusinessException`을 잡아 표준 에러 응답을 자동 반환하므로,
별도의 try-catch나 핸들러 추가 불필요.

---

## 예외 처리 구조

```
BusinessException(ExceptionType)
        ↓
GlobalExceptionHandler (@RestControllerAdvice)
        ↓
ResponseEntity<ResponseBody<Void>> {
    status: ExceptionType.getStatus(),
    body: ResponseUtil.createFailureResponse(exceptionType)
}
```

그 외 자동 처리되는 예외:
- `MethodArgumentNotValidException` → `BINDING_ERROR` + 검증 메시지
- `AuthorizationDeniedException` → `ACCESS_DENIED`
- `Exception` (기타) → `UNEXPECTED_SERVER_ERROR`
