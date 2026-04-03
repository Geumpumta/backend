# FCM Domain

Firebase Cloud Messaging 기반 푸시 알림 토큰 관리 및 알림 발송을 담당하는 도메인.

---

## 1. 절대 규칙

- **FCM 알림 실패가 핵심 비즈니스 로직을 중단시키면 안 됨** — 알림 발송은 항상 try-catch로 감싸고 예외를 로깅만 할 것
- **토큰 중복 금지** — 하나의 FCM 토큰은 하나의 사용자에게만 바인딩. 등록 시 기존 보유자에서 제거 후 할당
- **Firebase 서비스 계정 JSON은 git submodule(`security/`)로 관리** — 직접 커밋 금지
- **`PermanentFcmException`에 해당하는 에러는 재시도하지 않음** — UNREGISTERED, INVALID_ARGUMENT, SENDER_ID_MISMATCH, THIRD_PARTY_AUTH_ERROR

---

## 2. 아키텍처

### 파일 구조
```
fcm/
├── api/
│   └── FcmApi.java                # Swagger 문서
├── controller/
│   └── FcmController.java        # POST /token (등록), DELETE /token (삭제)
├── dto/
│   ├── FcmMessageDto.java         # 메시지 DTO (token, title, body, imageUrl, data)
│   └── request/
│       └── FcmTokenRequest.java   # 토큰 등록 요청 (@NotBlank fcmToken)
├── exception/
│   └── PermanentFcmException.java # 재시도 불가 Firebase 에러 래퍼
└── service/
    ├── FcmService.java            # 토큰 관리 + 알림 오케스트레이션
    └── FcmMessageSender.java      # Firebase 메시지 발송 (@Retryable)

# 관련 설정
global/config/fcm/
├── FcmConfig.java                 # FirebaseApp 초기화 (@Profile("!test"))
└── FcmProperties.java             # firebase.serviceAccountPath, firebase.projectId
```

### 토큰 저장
- `User.fcmToken` (VARCHAR 255, nullable) — 별도 엔티티 없이 User에 직접 저장
- 1 User : 1 Token (단일 디바이스 바인딩)

### 토큰 생명주기
| 이벤트 | 동작 |
|--------|------|
| `POST /api/v1/fcm/token` | 기존 보유자에서 제거 → 새 사용자에 할당 |
| `DELETE /api/v1/fcm/token` | 토큰 null 처리 |
| 로그아웃 (`UserService.logout`) | `fcmService.removeFcmToken()` 호출 |
| 회원 탈퇴 (`UserService.withdrawUser`) | `fcmService.removeFcmToken()` + `@SQLDelete`로 fcm_token=NULL |
| Firebase `UNREGISTERED` 응답 | 로그 경고 (자동 삭제는 미구현) |

### 재시도 전략 (`FcmMessageSender.send`)
```
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
```
- **재시도 대상**: UNAVAILABLE, INTERNAL, QUOTA_EXCEEDED
- **재시도 불가** (`PermanentFcmException`): UNREGISTERED, INVALID_ARGUMENT, SENDER_ID_MISMATCH, THIRD_PARTY_AUTH_ERROR
- **복구**: 3회 실패 시 `@Recover` → `BusinessException(FCM_SEND_FAILED)` throw

### 알림 트리거 (현재 1건)

**최대 집중 시간 초과 알림**:
1. `MaxFocusStudyScheduler` (`@Scheduled(fixedRate = 1000)`) — 3시간 초과 세션 감지
2. `StudySessionService.endExpiredMaxFocusSessions()` — 세션 강제 종료
3. `FcmService.sendMaxFocusNotification(user, hours)` — 푸시 발송
   - title: "최대 집중 시간 도달"
   - body: "{hours}시간 동안 열심히 공부하셨습니다! 잠시 휴식을 취해보세요."
   - data: `{ type: "STUDY_SESSION_FORCE_ENDED", maxFocusHours: "{hours}" }`
   - 토큰 없으면 silent return, 예외 발생해도 catch 후 로깅만

---

## 3. 빌드 & 테스트

### 테스트 설정
- `TestFcmConfig` (`@Profile("test")`) — 더미 credentials로 mock FirebaseApp 생성
- `FcmConfig`는 `@Profile("!test")`로 테스트 시 비활성화
- FCM 전용 단위/통합 테스트 파일은 현재 없음 — study 도메인 통합 테스트에서 간접 검증

---

## 4. 도메인 컨텍스트

### 타 도메인 의존 관계
```
study (MaxFocusStudyScheduler) → FcmService.sendMaxFocusNotification()
user  (UserService.logout/withdraw) → FcmService.removeFcmToken()
user  (UserRepository.findByFcmToken) → 토큰 중복 체크
```

### 에러 코드
| 코드 | 이름 | HTTP | 설명 |
|------|------|------|------|
| F001 | FCM_SEND_FAILED | 500 | 푸시 알림 전송 실패 (3회 재시도 후) |
| F002 | FCM_INVALID_TOKEN | 400 | 빈/유효하지 않은 FCM 토큰 |
| F003 | FCM_TOKEN_NOT_FOUND | 404 | 등록된 FCM 토큰 없음 |

### API 엔드포인트 (`/api/v1/fcm`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/token` | FCM 디바이스 토큰 등록 | USER |
| DELETE | `/token` | FCM 토큰 삭제 | USER |

모든 엔드포인트: `@PreAuthorize(USER)` + `@AssignUserId`

---

## 5. 코딩 컨벤션

1. 새 알림 타입 추가 시 `FcmService`에 전용 메서드 생성 (예: `sendMaxFocusNotification` 패턴)
2. 알림 data 필드의 `type` 키로 클라이언트 측 분기 — 새 타입 추가 시 클라이언트 팀과 협의
3. 재시도 불가 에러 추가 시 `FcmMessageSender.send()`의 `isPermanentError()` 분기에 추가
4. 응답은 `ResponseUtil.createSuccessResponse()` 표준 형식
5. 알림 발송은 핵심 로직과 분리 — 발송 실패가 트랜잭션을 롤백시키지 않도록 설계
