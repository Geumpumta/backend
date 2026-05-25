# Auth Requirements

## 목적

Flutter 모바일 앱에서 OAuth2 소셜 로그인과 JWT 기반 인증을 사용하되, 서버가 로그인 세션을 제어할 수 있도록 `JWT + Session Store` 혼합 구조를 사용한다.

## 핵심 구조

- Access Token은 API 인증용 JWT로 발급한다.
- Access Token은 짧은 만료 시간을 가져야 한다.
- Access Token에는 사용자 식별자와 서버 세션 식별자인 `sessionId`가 포함되어야 한다.
- Refresh Token은 Access Token 재발급용 토큰이다.
- Refresh Token은 서버의 Session Store에 저장되고 특정 세션과 연결되어야 한다.
- 로그인 상태 판단은 Refresh Token 존재 여부가 아니라 Session Store의 세션 상태를 기준으로 해야 한다.
- Session Store는 최소한 `sessionId`, `userId`, `status`, `refreshToken`, `lastSeenAt`, `expiresAt` 정보를 관리할 수 있어야 한다.
- 세션 상태는 최소한 `ACTIVE`, `REVOKED`를 구분할 수 있어야 한다.

## 기능 요구사항

- 사용자는 Google, Kakao, Apple OAuth2 로그인을 통해 인증할 수 있어야 한다.
- OAuth 로그인 성공 시 서버는 새로운 `sessionId`를 생성해야 한다.
- 새 로그인 성공 시 같은 사용자의 기존 `ACTIVE` 세션은 `REVOKED` 처리되어야 한다.
- 새 로그인 성공 시 서버는 새 세션과 연결된 Refresh Token을 저장해야 한다.
- 새 로그인 성공 시 서버는 `sessionId`가 포함된 Access Token과 Refresh Token을 발급해야 한다.
- 클라이언트는 Access Token을 `Authorization: Bearer <accessToken>` 헤더로 전달해야 한다.
- 인증이 필요한 API 요청은 JWT 검증과 세션 상태 검증을 모두 통과해야 한다.
- Access Token 자체가 서명/만료 기준으로 유효하더라도 연결된 세션이 `ACTIVE`가 아니면 요청은 거부되어야 한다.
- Access Token이 만료된 경우 클라이언트는 Refresh Token으로 Access Token을 재발급받을 수 있어야 한다.
- 토큰 재발급 시 Refresh Token과 연결된 세션이 `ACTIVE`여야 한다.
- 재로그인 시 기존 세션은 `REVOKED` 처리되고 새 세션이 생성되어야 한다.
- 로그아웃 시 현재 세션은 `REVOKED` 처리되고 해당 Refresh Token은 무효화되어야 한다.
- 로그아웃 시 현재 기기와 연결된 FCM Token은 무효화되어야 한다.
- 회원 탈퇴 또는 계정 비활성화 성격의 작업은 해당 사용자의 모든 세션을 `REVOKED` 처리할 수 있어야 한다.
- 탈퇴 상태의 사용자는 복구 API를 제외한 인증 API를 사용할 수 없어야 한다.

## 인증 흐름

### 로그인

1. 클라이언트가 OAuth2 로그인을 시작한다.
2. OAuth provider 인증 성공 후 서버 callback으로 돌아온다.
3. 서버는 사용자 정보를 확인한다.
4. 서버는 같은 사용자의 기존 `ACTIVE` 세션을 `REVOKED` 처리한다.
5. 서버는 새 `sessionId`를 생성하고 `ACTIVE` 세션을 저장한다.
6. 서버는 `sessionId`가 포함된 Access Token과 세션에 연결된 Refresh Token을 발급한다.
7. Flutter 앱은 발급받은 토큰을 안전한 저장소에 저장한다.

### API 요청

1. 클라이언트는 `Authorization: Bearer <accessToken>` 헤더로 API를 호출한다.
2. 서버는 Access Token의 서명과 만료 시간을 검증한다.
3. 서버는 Access Token에서 `sessionId`를 추출한다.
4. 서버는 Session Store에서 해당 세션이 `ACTIVE`인지 확인한다.
5. 세션이 `ACTIVE`이면 요청을 허용한다.

### 토큰 재발급

1. 클라이언트는 Access Token과 Refresh Token으로 재발급을 요청한다.
2. 서버는 Access Token에서 사용자 식별자와 `sessionId`를 확인한다.
3. 서버는 Refresh Token이 해당 세션에 연결된 토큰인지 확인한다.
4. 서버는 해당 세션이 `ACTIVE`인지 확인한다.
5. 조건을 만족하면 새 Access Token을 발급한다.

### 재로그인

1. 사용자가 새 기기 또는 재설치된 앱에서 로그인한다.
2. 서버는 기존 `ACTIVE` 세션을 `REVOKED` 처리한다.
3. 서버는 새 세션과 새 Refresh Token을 생성한다.
4. 기존 기기의 Access Token은 JWT 자체가 만료 전이어도 세션 상태 검증에서 거부된다.

### 로그아웃

1. 서버는 현재 요청의 `sessionId`를 기준으로 현재 세션을 찾는다.
2. 현재 세션을 `REVOKED` 처리한다.
3. 현재 세션에 연결된 Refresh Token을 무효화한다.
4. 현재 기기와 연결된 FCM Token을 무효화한다.

## 비기능 요구사항

- 유저당 동시에 활성화될 수 있는 로그인 기기는 1개로 제한되어야 한다.
- 기존 로그인 세션은 서버에서 즉시 제어하고 무효화할 수 있어야 한다.
- 앱 삭제/재설치로 클라이언트의 Refresh Token이 유실되어도 사용자가 영구적으로 재로그인할 수 없는 상태가 되면 안 된다.
- 새 기기 로그인 또는 재로그인 시 기존 기기의 Access Token은 만료 전이라도 즉시 사용할 수 없어야 한다.
- 인증 상태는 클라이언트 저장 토큰 존재 여부가 아니라 서버의 세션 상태를 기준으로 판단되어야 한다.
- Flutter 모바일 앱 환경에서 쿠키 기반 세션 관리 없이 Authorization Header 기반 인증으로 안정적으로 동작해야 한다.
- OAuth redirect 이후에도 모바일 앱이 Access Token과 Refresh Token을 안정적으로 수신하고 저장할 수 있어야 한다.
- 회원탈퇴, 개인정보 수정 같은 민감 API는 일반 API보다 낮은 수준의 인증 검증으로 처리되어서는 안 된다.
- Session Store 장애나 조회 실패가 인증 우회로 이어져서는 안 되며, 안전하게 인증 실패로 처리되어야 한다.
- 인증 실패 응답은 클라이언트가 재로그인 필요, 토큰 재발급 필요, 권한 부족을 구분할 수 있어야 한다.

## 예외/제약사항

- 순수 Stateless JWT 구조는 사용하지 않는다.
- JWT 검증만으로 인증 성공 처리해서는 안 된다.
- 인증 성공 조건은 JWT 서명 유효, JWT 만료 전, 세션 `ACTIVE` 상태를 모두 만족해야 한다.
- 기존 Refresh Token이 서버에 남아 있어도 새 OAuth 로그인을 차단해서는 안 된다.
- 앱 삭제 등으로 클라이언트 Refresh Token이 유실된 경우에도 재로그인을 통해 기존 서버 세션을 대체할 수 있어야 한다.
- `REVOKED` 세션에 연결된 Access Token과 Refresh Token은 사용할 수 없어야 한다.
- 존재하지 않거나 일치하지 않는 Refresh Token으로 재발급을 요청하면 재발급이 실패해야 한다.
- 회원탈퇴, 개인정보 수정 등 민감 API는 일반 인증 API와 동일하게 세션 `ACTIVE` 검증을 반드시 수행해야 한다.

## 관련 API

- `GET /oauth2/authorization/{provider}`: OAuth 로그인 시작
- `GET /login/oauth2/code/{provider}`: OAuth provider redirect callback
- `POST /auth/token/refresh`: Access Token 재발급
- `DELETE /api/v1/user/logout`: 현재 세션 로그아웃
- `DELETE /api/v1/user/withdraw`: 회원 탈퇴 및 전체 세션 무효화
- `POST /api/v1/user/restore`: 탈퇴 계정 복구

## 검증 시나리오

- A 기기에서 로그인하면 `ACTIVE` 세션과 `sessionId`가 포함된 Access Token이 발급된다.
- B 기기에서 같은 계정으로 로그인하면 A 기기의 기존 세션은 `REVOKED` 처리되고 B 기기의 새 세션만 `ACTIVE`가 된다.
- A 기기의 Access Token이 만료 전이어도 A 세션이 `REVOKED`이면 인증 API 호출은 실패한다.
- B 기기의 Access Token과 `ACTIVE` 세션으로 인증 API 호출은 성공한다.
- `REVOKED` 세션의 Refresh Token으로 토큰 재발급을 요청하면 실패한다.
- 앱 삭제로 기존 Refresh Token을 잃어버린 사용자가 다시 OAuth 로그인하면 기존 세션이 대체되고 새 토큰을 받을 수 있다.
- 로그아웃하면 현재 세션이 `REVOKED` 처리되고 같은 세션의 Refresh Token은 더 이상 사용할 수 없다.
- 회원 탈퇴 시 사용자의 모든 세션이 `REVOKED` 처리된다.
