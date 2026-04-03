---
name: spring-test
description: >
  Geumpumta Spring Boot 백엔드 테스트 코드 작성 스킬.
  Service 단위 테스트(JUnit 5 + Mockito + AssertJ)와 Controller 통합 테스트(TestContainers + MockMvc)를 작성한다.
  사용자가 "테스트 작성", "단위 테스트", "통합 테스트", "Service 테스트", "Controller 테스트", "테스트 코드",
  "Unit Test", "Integration Test", "Test 추가해줘" 등을 요청할 때 반드시 이 스킬을 사용할 것.
  새 도메인이나 기능 구현 후 테스트가 필요할 때도 자동으로 트리거할 것.
---

# Spring Test — Geumpumta 테스트 코드 작성 가이드

---

## 테스트 종류 선택

| 요청 | 작성할 테스트 |
|------|------------|
| Service 로직 검증 | 단위 테스트 (`unit/`) |
| API 엔드포인트 검증 | 통합 테스트 (`integration/`) |
| 둘 다 요청 | 둘 다 작성 |

---

## 1. 단위 테스트 (Service)

### 위치
```
src/test/java/com/gpt/geumpumtabackend/unit/{도메인}/service/
└── {도메인}ServiceTest.java
```

### 클래스 구조

`BaseUnitTest`를 **상속하지 않는다**. `@ExtendWith(MockitoExtension.class)`만 사용한다.
(BaseUnitTest는 @SpringBootTest를 로드하므로 순수 Mockito 테스트엔 불필요하다.)

```java
package com.gpt.geumpumtabackend.unit.{도메인}.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
// ... 필요한 import

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("{도메인}Service 단위 테스트")
class {도메인}ServiceTest {

    @Mock
    private {의존성}Repository {의존성}Repository;

    // 다른 @Mock 의존성...

    @InjectMocks
    private {도메인}Service {도메인}Service;

    // @Nested 클래스로 기능별 그룹화
}
```

### 테스트 메서드 패턴

**정상 케이스:**
```java
@Nested
@DisplayName("{기능명}")
class {기능명} {

    @Test
    @DisplayName("{상황}일 때 {결과}가 반환된다")
    void {상황}_결과반환() {
        // Given
        Long userId = 1L;
        User testUser = createTestUser(userId, "김철수", Department.SOFTWARE);

        given({mock}.{method}(any())).willReturn({value});

        // When
        {ResponseType} response = {service}.{method}({args});

        // Then
        assertThat(response).isNotNull();
        assertThat(response.{field}()).isEqualTo({expected});
        verify({mock}).{method}({matcher});
    }
}
```

**예외 케이스:**
```java
@Test
@DisplayName("{상황}일 때 {ERROR_CODE} 예외가 발생한다")
void {상황}_예외발생() {
    // Given
    given({mock}.{method}(any())).willReturn({errorValue});

    // When & Then
    assertThatThrownBy(() -> {service}.{method}({args}))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.{ERROR_CODE});

    verify({mock}, never()).{shouldNotCallMethod}(any());
}
```

### 테스트 데이터 헬퍼

엔티티의 `id` 필드는 JPA가 관리하므로 Reflection으로 설정한다:

```java
private User createTestUser(Long id, String name, Department department) {
    User user = User.builder()
            .name(name)
            .email("test@kumoh.ac.kr")
            .department(department)
            .picture("test.jpg")
            .role(UserRole.USER)
            .provider(OAuth2Provider.GOOGLE)
            .providerId("test-provider-id")
            .build();
    setId(user, id);
    return user;
}

private void setId(Object entity, Long id) {
    try {
        java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    } catch (Exception e) {
        throw new RuntimeException("Failed to set test entity ID", e);
    }
}
```

인터페이스 Projection(예: `DepartmentRankingTemp`)은 `mock()` + `given()`으로 처리한다:
```java
private DepartmentRankingTemp createMockProjection(String dept, Long millis, Long rank) {
    DepartmentRankingTemp m = mock(DepartmentRankingTemp.class);
    given(m.getDepartmentName()).willReturn(dept);
    given(m.getTotalMillis()).willReturn(millis);
    given(m.getRanking()).willReturn(rank);
    return m;
}
```

---

## 2. 통합 테스트 (Controller)

### 위치
```
src/test/java/com/gpt/geumpumtabackend/integration/{도메인}/controller/
└── {도메인}ControllerIntegrationTest.java
```

### 클래스 구조

반드시 `BaseIntegrationTest`를 상속한다. `@AfterEach`에서 자동으로 TRUNCATE + FLUSHALL 처리된다.

```java
package com.gpt.geumpumtabackend.integration.{도메인}.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpt.geumpumtabackend.integration.config.BaseIntegrationTest;
// ... 필요한 import

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("{도메인} Controller 통합 테스트")
@AutoConfigureMockMvc
class {도메인}ControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtHandler jwtHandler;

    @Autowired
    private UserRepository userRepository;

    // 테스트용 공유 데이터
    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testUser = createUser("테스트유저", "test@kumoh.ac.kr", Department.SOFTWARE);
        accessToken = generateToken(testUser);
    }

    private User createUser(String name, String email, Department department) {
        User user = User.builder()
                .name(name).email(email).department(department)
                .role(UserRole.USER).picture("profile.jpg")
                .provider(OAuth2Provider.GOOGLE).providerId("provider-" + email)
                .build();
        return userRepository.save(user);
    }

    private String generateToken(User user) {
        JwtUserClaim claim = new JwtUserClaim(user.getId(), UserRole.USER, false);
        return jwtHandler.createTokens(claim).getAccessToken();
    }
}
```

### HTTP 요청/응답 검증 패턴

**성공 케이스:**
```java
@Nested
@DisplayName("{API명} API")
class {API명}Api {

    @Test
    @DisplayName("정상 요청 시 {결과}를 반환한다")
    void 정상요청_결과반환() throws Exception {
        // Given
        {RequestType} request = new {RequestType}({args});

        // When & Then
        mockMvc.perform(post("/api/v1/{path}")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"))
                .andExpect(jsonPath("$.data.{field}").exists());

        // DB 직접 검증 (필요 시)
        {Entity} saved = {repository}.findAll().get(0);
        assertThat(saved.{field}()).isEqualTo({expected});
    }

    @Test
    @DisplayName("인증 없이 요청하면 403 에러가 발생한다")
    void 인증없음_403에러() throws Exception {
        mockMvc.perform(get("/api/v1/{path}"))
                .andExpect(status().isForbidden());
    }
}
```

**응답값을 다음 요청에 사용할 때:**
```java
String responseBody = mockMvc.perform(post("/api/v1/{path}")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

Long id = objectMapper.readTree(responseBody)
        .get("data")
        .get("{idField}")
        .asLong();
```

### 전체 흐름 테스트 (필수 포함)

시나리오 기반으로 여러 API를 순서대로 호출하는 흐름 테스트를 반드시 1개 포함한다:

```java
@Nested
@DisplayName("Controller-Service-Repository 전체 흐름 테스트")
class FullFlowTest {

    @Test
    @DisplayName("{시나리오} 전체 흐름이 정상 동작한다")
    void 전체흐름_정상동작() throws Exception {
        // 1단계: ...
        // 2단계: ...
        // 3단계: DB 최종 검증
    }
}
```

---

## 3. 응답 구조

모든 API는 `ResponseUtil`로 표준화된 형식을 사용한다:

```json
{
  "success": "true",
  "data": { ... }
}
```

실패 시:
```json
{
  "success": "false",
  "error": {
    "code": "ST001",
    "message": "..."
  }
}
```

JSONPath 검증 예시:
```java
.andExpect(jsonPath("$.success").value("true"))
.andExpect(jsonPath("$.data.{field}").exists())
.andExpect(jsonPath("$.data.{field}").isNumber())
.andExpect(jsonPath("$.data.{arrayField}").isArray())
.andExpect(jsonPath("$.data.{arrayField}", hasSize(3)))
.andExpect(jsonPath("$.data.{arrayField}[0].{subField}").value("expected"))
```

---

## 4. @AssignUserId AOP 처리

컨트롤러 메서드가 `@AssignUserId`를 사용하면 JWT에서 userId를 자동 주입한다.
통합 테스트에서는 유효한 JWT 토큰을 `Authorization: Bearer {token}` 헤더로 전달하면 자동으로 처리된다.
단위 테스트에서는 userId를 직접 파라미터로 전달한다.

---

## 5. 체크리스트

단위 테스트 작성 후 확인:
- [ ] `@ExtendWith(MockitoExtension.class)` 사용
- [ ] `@Mock` / `@InjectMocks` 올바른 위치
- [ ] Given-When-Then 구조
- [ ] 정상 케이스 + 예외 케이스 모두 포함
- [ ] `verify()`로 Mock 호출 검증
- [ ] `@Nested` + `@DisplayName` 한글 설명

통합 테스트 작성 후 확인:
- [ ] `BaseIntegrationTest` 상속
- [ ] `@AutoConfigureMockMvc` 선언
- [ ] `@BeforeEach`에서 테스트 데이터 + JWT 토큰 설정
- [ ] 인증 없는 요청 → 403 테스트 포함
- [ ] 전체 흐름 테스트 1개 이상 포함
- [ ] DB 직접 검증 (`repository.findAll()` 등)
