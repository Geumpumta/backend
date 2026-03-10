# WiFi Domain CLAUDE.md

## 개요

캠퍼스 Wi-Fi 네트워크 검증 도메인. 학습 세션 시작 시 사용자가 금오공대 캠퍼스 네트워크에 접속해 있는지 gateway IP와 client IP를 검증한다.

## 파일 구조

```
wifi/
├── config/
│   └── CampusWiFiProperties.java         # @ConfigurationProperties 설정
├── dto/
│   └── WiFiValidationResult.java         # 검증 결과 DTO
└── service/
    └── CampusWiFiValidationService.java  # 검증 서비스 (@Cacheable)
```

## 검증 흐름

```
validateCampusWiFi(gatewayIp, clientIp)
  → 캐시 확인 (key: "gatewayIp:clientIp")
  → 캐시 미스 시:
      → active 네트워크 목록 필터링
      → 각 네트워크에 대해:
          1. Gateway IP가 네트워크의 gatewayIps 목록에 포함? (불일치 → 다음 네트워크)
          2. Client IP가 네트워크의 ipRanges(CIDR) 내? (일치 → VALID)
      → 모두 불일치 → INVALID
  → 예외 발생 시 → ERROR
```

## 주요 클래스

### CampusWiFiProperties (Record)
`application-wifi.yml`의 `campus.wifi` 프리픽스에 바인딩.

```yaml
campus:
  wifi:
    networks:
      - name: "kit-main"
        gatewayIps: ["172.30.64.1"]
        ipRanges: ["172.30.0.0/16"]
        active: true
        description: "금오공대 메인 네트워크"
    validation:
      cacheTtlMinutes: 60
```

- `WiFiNetwork.isValidGatewayIP()`: gateway IP 목록에 포함 여부
- `WiFiNetwork.isValidIP()`: Apache Commons Net `SubnetUtils`로 CIDR 범위 검사
- `networks`가 null이면 빈 리스트, `validation`이 null이면 기본 60분 TTL

### WiFiValidationResult
3가지 상태를 가진 결과 DTO:

| 상태 | valid | 의미 |
|------|-------|------|
| `VALID` | true | 캠퍼스 네트워크 확인 |
| `INVALID` | false | 캠퍼스 네트워크 아님 (IP 대역 불일치) |
| `ERROR` | false | 시스템 오류 |

팩토리 메서드: `WiFiValidationResult.valid()`, `.invalid()`, `.error()`

### CampusWiFiValidationService
- `@Cacheable(value = "wifiValidation", key = "#gatewayIp + ':' + #clientIp")` 적용
- 캐시 설정은 `CacheConfig`에서 Caffeine으로 관리
- active가 false인 네트워크는 검증에서 제외

## 사용처

`StudySessionService.verifyCampusWifiConnection()`에서 호출:
```java
WiFiValidationResult result = wifiValidationService.validateCampusWiFi(gatewayIp, clientIp);
// INVALID → WIFI_NOT_CAMPUS_NETWORK 예외
// ERROR → WIFI_VALIDATION_ERROR 예외
```

## 테스트 (`CampusWiFiValidationServiceTest`)

5개 테스트 케이스:
- 유효한 네트워크 매칭 → 검증 성공
- 매칭되는 네트워크 없음 → 검증 실패
- Gateway IP 매칭, Client IP 범위 불일치 → 검증 실패
- 비활성화 네트워크 제외 확인
- 여러 네트워크 중 하나라도 매칭 → 검증 성공

`TestWiFiMockConfig`에서 테스트용 Wi-Fi 설정을 제공.

## 개발 시 주의사항

1. 네트워크 설정은 `application-wifi.yml`(security submodule)에서 관리 — 직접 커밋 금지
2. CIDR 형식 오류 시 `SubnetUtils`가 예외를 던짐 — `isIpInRange()`에서 catch 후 false 반환
3. 캐시 키가 `gatewayIp:clientIp` 조합 — 같은 IP 조합은 캐시 TTL 동안 재검증하지 않음
4. 새 캠퍼스 네트워크 추가 시 yml 설정만 변경하면 됨 (코드 수정 불필요)
