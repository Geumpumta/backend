# USE-CASES.md

현재 구현된 기능을 유즈케이스 단위로 정리한 문서. 총 **42개** 유즈케이스.

---

## 액터 정의

| 액터 | 설명 |
|------|------|
| GUEST | OAuth2 로그인 완료, 회원가입 미완료 (이메일 인증·학과 선택 전) |
| USER | 회원가입 완료 사용자 |
| ADMIN | 관리자 (USER 권한 포함) |
| SYSTEM | 스케줄러·내부 서비스 호출 |

---

## 1. 학습 세션 (Study)

### UC-ST-001 오늘의 학습 현황 조회
| 항목 | 내용 |
|------|------|
| 액터 | USER |
| 엔드포인트 | `GET /api/v1/study` |
| 설명 | 오늘 완료된 세션의 총 공부 시간 + 현재 진행 중 여부 반환 |
| 비즈니스 규칙 | FINISHED 세션만 합산, STARTED 세션은 isStudying 플래그로 표시 |

### UC-ST-002 학습 세션 시작
| 항목 | 내용 |
|------|------|
| 액터 | USER |
| 엔드포인트 | `POST /api/v1/study/start` |
| 전제조건 | 캠퍼스 Wi-Fi 접속 상태, 진행 중인 세션 없음 |
| 흐름 | Wi-Fi 검증 → 중복 세션 확인 → 세션 생성 (startTime=서버 시간, status=STARTED) |
| 비즈니스 규칙 | 클라이언트 타임스탬프 사용 금지, 1인 1세션 제한 |
| 에러코드 | `W001` `W002` `W003` `ST002` `U001` |

### UC-ST-003 학습 세션 종료
| 항목 | 내용 |
|------|------|
| 액터 | USER |
| 엔드포인트 | `POST /api/v1/study/end` |
| 흐름 | 세션 조회 → endTime=서버 시간 → totalMillis 계산 → status=FINISHED |
| 에러코드 | `ST001` `U001` |

### UC-ST-004 최대 집중시간 초과 자동 종료
| 항목 | 내용 |
|------|------|
| 액터 | SYSTEM |
| 트리거 | 매 10분 (`0 */10 * * * *`) |
| 흐름 | STARTED + 3시간 초과 세션 검색 → endTime=startTime+3h → FINISHED → FCM 알림 |
| 비즈니스 규칙 | FCM 실패해도 세션 종료는 진행, endTime은 현재 시간이 아닌 startTime+maxHours |

---

## 2. 개인 랭킹 (Personal Rank)

> **이중 랭킹 구조**: `date` 파라미터 없으면 실시간 계산 (Native Query), 있으면 확정 랭킹 조회 (UserRanking 테이블)

### UC-RK-001~006 개인 랭킹 조회 (일간/주간/월간 × 실시간/확정)

| UC | 엔드포인트 | 유형 | 기간 기준 |
|----|-----------|------|----------|
| 001 | `GET /personal/daily` | 실시간 | 오늘 00:00~23:59 |
| 002 | `GET /personal/daily?date=` | 확정 | 지정 날짜 |
| 003 | `GET /personal/weekly` | 실시간 | 이번 주 월~일 |
| 004 | `GET /personal/weekly?date=` | 확정 | 지정 주 (월요일 기준) |
| 005 | `GET /personal/monthly` | 실시간 | 이번 달 1일~말일 |
| 006 | `GET /personal/monthly?date=` | 확정 | 지정 월 (1일 기준) |

**공통 규칙:**
- 실시간: 진행 중 세션 포함 (startTime~now), LEAST/GREATEST로 기간 경계 처리
- 확정: 스케줄러가 저장한 UserRanking에서 조회
- 응답: 상위 랭킹 목록 + 본인 순위 (없으면 rank=listSize+1, totalMillis=0)

---

## 3. 학과 랭킹 (Department Rank)

### UC-RK-007~009 학과 랭킹 조회 (일간/주간/월간 × 실시간/확정)

| UC | 엔드포인트 | 유형 |
|----|-----------|------|
| 007 | `GET /department/daily` | 실시간 |
| 008 | `GET /department/daily?date=` | 확정 |
| 009 | `GET /department/{weekly,monthly}` | 실시간/확정 |

**학과 랭킹 계산 규칙:**
- 학과별 상위 30명의 공부 시간 합산
- Native Query + CTE, `ROW_NUMBER() PARTITION BY department` → 상위 30 필터 → SUM → RANK()
- 25개 학과 대상, 0시간 학과는 topRanks에서 제외 (본인 학과는 항상 포함)

---

## 4. 시즌 랭킹 (Season Rank)

### UC-RK-010 현재 시즌 전체 랭킹
| 항목 | 내용 |
|------|------|
| 액터 | USER |
| 엔드포인트 | `GET /api/v1/rank/season/current` |
| 흐름 | ①확정 월간 합산 + ②이번 달 일간 합산 + ③오늘 실시간 → 유저별 merge → 순위 부여 |
| 에러코드 | `SE001` `U001` |

### UC-RK-011 현재 시즌 학과별 랭킹
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `GET /api/v1/rank/season/current/department?department=` |
| 흐름 | UC-RK-010과 동일하나 학과 필터 적용 |

### UC-RK-012 종료 시즌 전체 랭킹
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `GET /api/v1/rank/season/{seasonId}` |
| 전제조건 | 시즌 status=ENDED |
| 흐름 | SeasonRankingSnapshot (rankType=OVERALL) 조회 |
| 에러코드 | `SE002` `SE003` |

### UC-RK-013 종료 시즌 학과별 랭킹
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `GET /api/v1/rank/season/{seasonId}/department?department=` |
| 흐름 | SeasonRankingSnapshot (rankType=DEPARTMENT) + 학과 필터 |

---

## 5. 랭킹 스케줄러 (Rank Scheduler)

### UC-RK-014 일간 랭킹 확정
| 항목 | 내용 |
|------|------|
| 트리거 | 매일 00:00:05 (`5 0 0 * * *`) |
| 흐름 | 전일 StudySession 계산 → UserRanking (DAILY) + DepartmentRanking (DAILY) 저장 |

### UC-RK-015 주간 랭킹 확정
| 항목 | 내용 |
|------|------|
| 트리거 | 매주 월요일 00:01 (`0 1 0 ? * MON`) |
| 흐름 | 전주 데이터 → UserRanking (WEEKLY) + DepartmentRanking (WEEKLY) 저장 |

### UC-RK-016 월간 랭킹 확정
| 항목 | 내용 |
|------|------|
| 트리거 | 매월 1일 00:02 (`0 2 0 1 * ?`) |
| 흐름 | 전월 데이터 → UserRanking (MONTHLY) + DepartmentRanking (MONTHLY) 저장 |

### UC-RK-017 시즌 전환 및 스냅샷 생성
| 항목 | 내용 |
|------|------|
| 트리거 | 매일 00:05 (`0 5 0 * * *`) |
| 전제조건 | today ≥ activeSeason.endDate + 1 |
| 흐름 | 캐시 클리어 → 현재 시즌 ENDED → 다음 시즌 ACTIVE → SeasonRankingSnapshot 배치 생성 |
| 비즈니스 규칙 | @Retryable 3회 (5초 backoff), JDBC 배치 2000건 청크, 중복 방지 체크 |
| 에러코드 | `SE001` `SE002` |

---

## 6. 통계 (Statistics)

> 모든 통계는 본인 또는 타 유저 조회 가능 (`targetUserId` 파라미터)

### UC-STAT-001 일간 통계
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `GET /api/v1/statistics/day?date=&targetUserId=` |
| 응답 | 2시간 슬롯 12개 (00~02, 02~04, ...) + 최대 집중 시간 + 총 공부 시간 |

### UC-STAT-002 주간 통계
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `GET /api/v1/statistics/week?date=&targetUserId=` |
| 응답 | 요일별 공부 시간 + 최대 집중 시간 |

### UC-STAT-003 월간 통계
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `GET /api/v1/statistics/month?date=&targetUserId=` |
| 응답 | 일별 공부 시간 집계 |

### UC-STAT-004 잔디 차트
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `GET /api/v1/statistics/grass?date=&targetUserId=` |
| 응답 | 5개월 범위 (전 3개월~다음 1개월) 일별 공부 기록 |

---

## 7. 사용자 (User)

### UC-US-001 회원가입 완료
| 항목 | 내용 |
|------|------|
| 액터 | GUEST |
| 엔드포인트 | `POST /api/v1/user/complete-registration` |
| 전제조건 | 이메일 인증 완료 (UC-US-006) |
| 흐름 | schoolEmail·studentId·department 저장 → 랜덤 닉네임 생성 → GUEST→USER 승격 → 새 JWT 발급 |
| 비즈니스 규칙 | 닉네임 = {형용사}{명사}{1~100}, 중복 시 재생성 |

### UC-US-002 프로필 조회
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `GET /api/v1/user/profile` |
| 응답 | nickname, email, department, picture 등 |

### UC-US-003 닉네임 중복 확인
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `GET /api/v1/user/nickname/verify?nickname=` |
| 응답 | 사용 가능 여부 (boolean) |

### UC-US-004 프로필 수정
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `POST /api/v1/user/profile` |
| 흐름 | imageUrl·publicId·nickname 업데이트 |

### UC-US-005 이메일 인증코드 요청
| 항목 | 내용 |
|------|------|
| 액터 | GUEST |
| 엔드포인트 | `POST /api/v1/email/request-code` |
| 흐름 | 6자리 랜덤 코드 생성 → Redis 저장 (TTL 5분) → 이메일 발송 |
| 비즈니스 규칙 | @kumoh.ac.kr 이메일만 허용, Redis 키: `{userId}email:{email}` |
| 에러코드 | `M001` |

### UC-US-006 이메일 인증코드 검증
| 항목 | 내용 |
|------|------|
| 액터 | GUEST |
| 엔드포인트 | `POST /api/v1/email/verify-code` |
| 흐름 | Redis에서 코드 조회 → 일치 시 삭제 (일회용) → 성공/실패 boolean 반환 |

### UC-US-007 로그아웃
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `DELETE /api/v1/user/logout` |
| 흐름 | RefreshToken 전체 삭제 + FCM 토큰 제거 |

### UC-US-008 회원 탈퇴 (Soft Delete)
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `DELETE /api/v1/user/withdraw` |
| 흐름 | RefreshToken 삭제 + FCM 제거 + @SQLDelete 마스킹 (필드 앞에 `deleted_` 접두사) |
| 비즈니스 규칙 | 데이터 보존, unique 제약 유지하면서 재가입 허용 |

### UC-US-009 탈퇴 복구
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `POST /api/v1/user/restore` |
| 흐름 | `deleted_` 접두사 제거 → deletedAt 초기화 → 새 JWT 발급 |

---

## 8. 토큰 (Token)

### UC-TK-001 토큰 갱신
| 항목 | 내용 |
|------|------|
| 액터 | 인증 불필요 |
| 엔드포인트 | `POST /auth/token/refresh` |
| 흐름 | accessToken 디코딩 → refreshToken DB 매칭 → 기존 삭제 → 새 토큰 쌍 발급 |
| 에러코드 | `S005` `T001` `T002` |

---

## 9. 게시판 (Board)

### UC-BD-001~004

| UC | 엔드포인트 | 액터 | 설명 |
|----|-----------|------|------|
| 001 | `GET /board/list` | USER | 최근 10건 목록 조회 |
| 002 | `GET /board/{id}` | USER | 상세 조회 |
| 003 | `POST /board` | ADMIN | 공지 작성 |
| 004 | `DELETE /board/{id}` | ADMIN | 공지 삭제 (soft delete) |

---

## 10. 이미지 (Image)

### UC-IM-001 프로필 이미지 업로드
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `POST /api/v1/image/profile` |
| 흐름 | 파일 검증 (빈 파일/크기/타입) → Cloudinary 업로드 → URL 반환 |
| 비즈니스 규칙 | 최대 10MB, JPEG·PNG·WebP·GIF만 허용, 실패 시 업로드 롤백 시도 |
| 에러코드 | `I001` `I002` `I003` |

---

## 11. FCM (Firebase Cloud Messaging)

### UC-FC-001 디바이스 토큰 등록
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `POST /api/v1/fcm/register` |
| 흐름 | fcmToken을 User 엔티티에 저장 (1인 1토큰, 덮어쓰기) |
| 에러코드 | `F001` |

### UC-FC-002 디바이스 토큰 삭제
| 항목 | 내용 |
|------|------|
| 엔드포인트 | `DELETE /api/v1/fcm/token` |
| 흐름 | User.fcmToken = null |

### UC-FC-003 최대 집중시간 알림 발송
| 항목 | 내용 |
|------|------|
| 액터 | SYSTEM (UC-ST-004에서 호출) |
| 흐름 | fcmToken 존재 시 푸시 발송, 실패해도 예외 미전파 |

---

## 12. 인증 (OAuth2 / Auth)

### UC-AU-001 OAuth2 소셜 로그인
| 항목 | 내용 |
|------|------|
| 액터 | 미인증 사용자 |
| 진입점 | `/oauth2/authorization/{kakao,google,apple}` |
| 흐름 | Provider 인증 → User 조회/생성 (GUEST) → JWT 발급 → redirect_uri로 토큰 전달 |
| 비즈니스 규칙 | 최초 로그인 시 GUEST 생성, 탈퇴 유저는 withdrawn 표시, redirect_uri 화이트리스트 검증 |

### UC-AU-002 만료 리프레시 토큰 정리
| 항목 | 내용 |
|------|------|
| 트리거 | 매일 00:00 (`0 0 0 * * *`) |
| 흐름 | expiredAt < now인 RefreshToken 일괄 삭제 |

---

## 유즈케이스 요약

| 도메인 | 수 | API | 스케줄러 | 내부 호출 |
|--------|-----|-----|---------|----------|
| Study | 4 | 3 | 1 | — |
| Personal Rank | 6 | 6 | — | — |
| Department Rank | 3 | 6 | — | — |
| Season Rank | 4 | 4 | — | — |
| Rank Scheduler | 4 | — | 4 | — |
| Statistics | 4 | 4 | — | — |
| User | 9 | 9 | — | — |
| Token | 1 | 1 | — | — |
| Board | 4 | 4 | — | — |
| Image | 1 | 1 | — | — |
| FCM | 3 | 2 | — | 1 |
| Auth | 2 | 1 | 1 | — |
| WiFi | 1 | — | — | 1 |
| **합계** | **46** | **41** | **6** | **2** |
