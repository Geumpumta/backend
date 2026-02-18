# TESTING.md

이 프로젝트의 테스트 전략, 작성 규칙, 커버리지 기준을 정의한다.

---

## 테스트 원칙

1. **핵심 비즈니스 로직은 반드시 단위 테스트한다** — 시간 계산, 랭킹 병합, 인증, 상태 전이
2. **외부 경계는 통합 테스트한다** — HTTP 요청/응답, 인증 필터, Native Query 정합성
3. **단순 위임/CRUD는 테스트하지 않는다** — `repository.save()` 호출만 하는 메서드, getter/setter
4. **외부 서비스는 Mock한다** — Cloudinary, FCM, SMTP를 실제 호출하지 않음
5. **테스트가 실패하면 배포하지 않는다** — CI에서 전체 테스트 통과 필수

---

## 커버리지 기준

| 계층 | 목표 | 기준 |
|------|------|------|
| Service (핵심 로직) | **85%** | 모든 public 메서드 + 분기 커버 |
| Domain Entity (상태 전이) | **90%** | 도메인 메서드 전체 |
| Controller (통합) | **70%** | 정상 + 인증실패 + 주요 예외 |
| Scheduler (로직) | **80%** | 계산 로직 + 예외 처리 |
| Repository (복잡 쿼리) | **60%** | Native Query, CTE 정합성 |

### 도메인별 우선순위

| 순위 | 도메인 | 유형 | 이유 |
|------|--------|------|------|
| **P0** | study, rank, token, user | Unit + Integration | 핵심 기능, 계산 정확성, 보안 |
| **P1** | scheduler, email, statistics | Unit | 데이터 무결성, 인증 |
| **P2** | wifi, image | Unit | 이미 완성 or 외부 서비스 래핑 |
| **P3** | board | 선택 | 단순 CRUD |

---

## 테스트 구조

```
src/test/java/com/gpt/geumpumtabackend/
├── unit/                              # Mockito + H2
│   ├── config/
│   │   └── BaseUnitTest.java          # 모든 단위 테스트 상속
│   └── {domain}/service/
│       └── {Domain}ServiceTest.java
└── integration/                       # TestContainers (MySQL 8.0 + Redis 7.0)
    ├── config/
    │   └── BaseIntegrationTest.java   # TRUNCATE + FLUSHALL 격리
    └── {domain}/controller/
        └── {Domain}ControllerIntegrationTest.java
```

---

## 단위 테스트

### 작성 대상

| 테스트한다 | 테스트하지 않는다 |
|-----------|-----------------|
| 조건 분기가 있는 비즈니스 로직 | 단순 getter/setter |
| 계산 로직 (시간, 랭킹 합산) | `repository.save()` 호출만 하는 메서드 |
| 상태 전이 (STARTED→FINISHED) | `@ConfigurationProperties` 바인딩 |
| 예외 발생 조건 | 외부 SDK 래핑 (Cloudinary upload) |
| 데이터 병합/변환, fallback | 단순 위임 메서드 |

### 작성 패턴

```java
class ExampleServiceTest extends BaseUnitTest {

    @InjectMocks private ExampleService exampleService;
    @Mock private ExampleRepository exampleRepository;

    @Nested
    @DisplayName("기능 그룹")
    class MethodGroup {

        @Test
        @DisplayName("정상 — 설명")
        void shouldReturnResult_whenValidInput() {
            // given
            given(exampleRepository.findById(1L)).willReturn(Optional.of(entity));
            // when
            var result = exampleService.getExample(1L);
            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("expected");
        }

        @Test
        @DisplayName("예외 — 설명")
        void shouldThrow_whenNotFound() {
            // given
            given(exampleRepository.findById(999L)).willReturn(Optional.empty());
            // when & then
            assertThatThrownBy(() -> exampleService.getExample(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getExceptionType())
                .isEqualTo(ExceptionType.EXAMPLE_NOT_FOUND);
        }
    }
}
```

### 네이밍

- 클래스: `{대상}Test` — `StudySessionServiceTest`
- 메서드: `should{결과}_when{조건}` — `shouldThrow_whenSessionAlreadyExists`
- `@DisplayName`: 한글 — `"예외 — 이미 진행중인 세션이 있을 때"`
- `@Nested`: 기능 그룹 — `class 학습시작`, `class 학습종료`

---

## 통합 테스트

### 작성 대상

| 테스트한다 | 테스트하지 않는다 |
|-----------|-----------------|
| Controller 인증/응답 형식 | 단순 Service 로직 (단위로 충분) |
| Native Query / CTE 정합성 | 외부 API 호출 |
| 인증 필터 체인 (JWT→PreAuthorize→AssignUserId) | |

### 작성 패턴

```java
class ExampleControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtHandler jwtHandler;
    private String accessToken;

    @BeforeEach
    void setUp() {
        var user = userRepository.save(User.builder()
            .email("test@kumoh.ac.kr").role(UserRole.USER)
            .nickname("tester").provider(OAuth2Provider.KAKAO)
            .providerId("id").department(Department.COMPUTER_ENGINEERING).build());
        accessToken = jwtHandler.createTokens(
            new JwtUserClaim(user.getId(), UserRole.USER, false)).getAccessToken();
    }

    @Test
    @DisplayName("200 — 정상 조회")
    void shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/example")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("401 — 토큰 없음")
    void shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/example"))
            .andExpect(status().isUnauthorized());
    }
}
```

### 엔드포인트별 최소 검증

| 케이스 | 검증 |
|--------|------|
| 정상 요청 | 200, `success: true`, 데이터 구조 |
| 인증 없음 | 401 |
| 주요 예외 | 4xx, `success: false`, 에러 코드 |
| ADMIN 전용 | USER로 접근 시 403 |

---

## 도메인별 테스트 필요 항목

### P0 — 반드시 작성

| 서비스 | 테스트할 핵심 로직 | 유형 |
|--------|-------------------|------|
| `StudySessionService` | 세션 시작(중복 방지), 종료(시간 계산), 최대시간 자동종료 | Unit |
| `PersonalRankService` | 실시간/확정 분기, fallback, 동점 처리 | Unit |
| `DepartmentRankService` | Top30 합산, 0시간 필터링, 본인 학과 포함 | Unit |
| `SeasonRankService` | 3중 병합(월간+일간+실시간), 종료 시즌 스냅샷 | Unit |
| `SeasonService` | 4시즌 순환 전환, 날짜 검증, 윤년 | Unit |
| `TokenService` | 토큰 갱신(만료 토큰 파싱, 리프레시 매칭, 재발급) | Unit |
| `UserService` | GUEST→USER 승격, 탈퇴(prefix), 복구(prefix 제거) | Unit |
| `StudySessionController` | 시작/종료/조회 E2E, 인증 | Integration |
| `RankController` (전체) | 일간/주간/월간 랭킹 응답 구조, 인증 | Integration |
| `UserController` | 가입 완료, 프로필, 탈퇴/복구 | Integration |
| `TokenController` | 토큰 갱신 응답 | Integration |

### P1 — 권장

| 서비스 | 테스트할 핵심 로직 | 유형 |
|--------|-------------------|------|
| `RankingSchedulerService` | 기간 계산(어제/전주/전월), 랭킹 저장 | Unit |
| `SeasonTransitionScheduler` | 종료일 판단, 캐시 clear, 전환+스냅샷 순서 | Unit |
| `SeasonSnapshotService` | 재시도(3회), 중복 방지, 배치 인서트 | Unit |
| `EmailService` | 인증코드 Redis 저장/검증/만료 | Unit |
| `StatisticsService` | 2시간 슬롯, 잔디 차트 데이터 가공 | Unit |

### P2~P3 — 선택

| 서비스 | 비고 |
|--------|------|
| `CampusWiFiValidationService` | 이미 5개 테스트 완성, 추가 불필요 |
| `ImageService` | Mock 기반 — 파일 검증, 크기 초과, 업로드 실패 롤백 |
| `FcmService` | Mock 기반 — 토큰 없는 사용자 skip, 발송 실패 처리 |
| `BoardService` | 단순 CRUD, ADMIN 권한 통합 테스트만 고려 |

---

## 테스트 환경

| 항목 | 단위 (unit-test) | 통합 (test) |
|------|------------------|-------------|
| DB | H2 인메모리 | TestContainers MySQL 8.0 |
| Redis | 비활성 | TestContainers Redis 7.0 |
| 외부 서비스 | `@Mock` | `@MockBean` |
| 베이스 클래스 | `BaseUnitTest` | `BaseIntegrationTest` |
| 테스트 격리 | Mockito 초기화 | TRUNCATE ALL + FLUSHALL |
| JVM 옵션 | `-XX:+EnableDynamicAgentLoading` | 동일 |

---

## 실행 명령

```bash
./gradlew test                                          # 전체
./gradlew test --tests "com.gpt.geumpumtabackend.unit.*"        # 단위만
./gradlew test --tests "com.gpt.geumpumtabackend.integration.*" # 통합만
./gradlew test --tests "StudySessionServiceTest"                 # 특정 클래스
./gradlew test --tests "StudySessionServiceTest.shouldThrow*"    # 특정 메서드
```

---

## 체크리스트: 새 기능 추가 시

```
□ Service에 단위 테스트 작성
  □ 정상 케이스 (최소 1개)
  □ 예외 케이스 (BusinessException 조건 전부)
  □ 경계값 (null, 빈 리스트, 0)
□ 엔티티에 상태 전이/계산 로직이 있으면 테스트
□ Controller에 통합 테스트 작성 (200 + 401 + 주요 4xx)
□ Native Query 추가/수정 시 통합 테스트로 검증
□ 기존 테스트 깨지지 않음 확인 (./gradlew test)
```
