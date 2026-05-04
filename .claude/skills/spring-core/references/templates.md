# 코드 템플릿

각 레이어별 코드 작성 시 이 템플릿을 참고한다. `{Example}`은 실제 도메인명으로 치환.

---

## 1. 엔티티

```java
package com.gpt.geumpumtabackend.{도메인}.domain;

import com.gpt.geumpumtabackend.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Example extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Builder
    public Example(String name) {
        this.name = name;
    }
}
```

**규칙:**
- `@Setter` 금지 — 상태 변경은 도메인 메서드
- `@Builder`는 생성자에만 (클래스 레벨 금지)
- Soft Delete: `@SQLDelete` + `@Where` (User 엔티티 참고)
- 연관관계: `@ManyToOne(fetch = LAZY)` 기본

---

## 2. 요청 DTO

```java
public record ExampleRequest(
    @NotBlank(message = "이름은 필수입니다")
    String name
) {}
```

---

## 3. 응답 DTO

```java
public record ExampleResponse(
    Long id,
    String name,
    LocalDateTime createdAt
) {
    public static ExampleResponse from(Example example) {
        return new ExampleResponse(
            example.getId(),
            example.getName(),
            example.getCreatedAt()
        );
    }
}
```

**규칙:**
- 엔티티만 받으면 `from()`, 추가 데이터가 필요하면 `of()`
- 리스트용 / 상세 조회용 DTO 분리 (예: `BoardListResponse` vs `BoardResponse`)

---

## 4. Repository

```java
public interface ExampleRepository extends JpaRepository<Example, Long> {
    List<Example> findTop10ByOrderByCreatedAtDesc();
}
```

- 복잡한 쿼리: `@Query`(JPQL) 또는 Native Query
- `StudySessionRepository` Native Query 수정 시 랭킹/통계 영향 확인

---

## 5. Service

```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExampleService {

    private final ExampleRepository exampleRepository;
    private final UserRepository userRepository;

    public ExampleResponse getExample(Long userId, Long exampleId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));

        Example example = exampleRepository.findById(exampleId)
            .orElseThrow(() -> new BusinessException(ExceptionType.EXAMPLE_NOT_FOUND));

        return ExampleResponse.from(example);
    }

    @Transactional
    public ExampleResponse createExample(Long userId, ExampleRequest request) {
        userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));

        Example example = Example.builder()
            .name(request.name())
            .build();

        return ExampleResponse.from(exampleRepository.save(example));
    }
}
```

---

## 6. Controller

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/example")
public class ExampleController implements ExampleApi {

    private final ExampleService exampleService;

    @GetMapping("/{exampleId}")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<ExampleResponse>> getExample(
            Long userId,
            @PathVariable Long exampleId
    ) {
        ExampleResponse response = exampleService.getExample(userId, exampleId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @PostMapping
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<ExampleResponse>> createExample(
            Long userId,
            @RequestBody @Valid ExampleRequest request
    ) {
        ExampleResponse response = exampleService.createExample(userId, request);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }
}
```

- URL 패턴: `/api/v1/{도메인명}`

---

## 7. Swagger API 인터페이스

```java
public interface ExampleApi {

    @Operation(
        summary = "예시 조회 API",
        description = "USER 이상의 권한을 가진 사용자가 예시를 조회합니다."
    )
    @SwaggerApiResponses(
        success = @SwaggerApiSuccessResponse(
            response = ExampleResponse.class,
            description = "예시 조회 성공"),
        errors = {
            @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
            @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            @SwaggerApiFailedResponse(ExceptionType.EXAMPLE_NOT_FOUND)
        }
    )
    @GetMapping("/{exampleId}")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<ExampleResponse>> getExample(
        @Parameter(hidden = true) Long userId,
        @PathVariable Long exampleId
    );
}
```

- `@Parameter(hidden = true)` — userId는 Swagger에 노출하지 않음
- `@SwaggerApiFailedResponse`에 발생 가능한 `ExceptionType` 모두 명시

---

## 8. 페이징 조회

```java
// Controller
@GetMapping
public ResponseEntity<ResponseBody<GlobalPageResponse<ExampleResponse>>> getList(
    Long userId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size
) { ... }

// Service
Page<Example> examples = exampleRepository.findAll(PageRequest.of(page, size));
return GlobalPageResponse.from(examples.map(ExampleResponse::from));
```

---

## 9. Enum 필드 / 연관관계

```java
// Enum
@Enumerated(EnumType.STRING)
private ExampleStatus status;

// 연관관계
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;
```
