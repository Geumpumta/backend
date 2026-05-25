# Requirements

이 디렉터리는 금품타 백엔드의 기능 요구사항을 패키지/도메인 단위로 정리한다.

요구사항 문서는 구현 클래스보다 외부 동작, API 계약, 예외 조건, 검증 시나리오를 중심으로 작성한다. `JWT`, `OAuth2`, `Refresh Token`, `FCM`처럼 클라이언트 연동 방식에 영향을 주는 기술 요소는 포함하되, 내부 Service/Repository 구조나 락 방식은 구현 문서에서 다룬다.

## 문서 목록

- [auth.md](auth.md): OAuth 로그인, JWT 인증, Session Store, 1기기 로그인, 로그아웃
- [user.md](user.md): 회원가입 완료, 프로필, 닉네임, 이메일 인증, 탈퇴/복구
- [study.md](study.md): 학습 세션 시작/종료, Wi-Fi 검증, 자동 종료
- [statistics.md](statistics.md): 일/주/월/잔디형 학습 통계
- [rank.md](rank.md): 개인/학부/시즌 랭킹
- [board.md](board.md): 게시판
- [badge.md](badge.md): 배지
- [fcm.md](fcm.md): FCM 토큰 등록과 알림 발송
- [image.md](image.md): 이미지 업로드
- [maintenance.md](maintenance.md): 점검 모드
- [common.md](common.md): 공통 응답, 예외, 권한, 시간 기준

## 작성 규칙

각 요구사항 문서는 다음 구성을 기본으로 한다.

1. 목적
2. 기능 요구사항
3. 비기능 요구사항
4. 예외/제약사항
5. 관련 API
6. 검증 시나리오

로그인/인증처럼 여러 패키지에 걸친 요구사항은 [auth.md](auth.md)에 모으고, 다른 문서에서는 필요할 때 참조한다.

## 요구사항 구분

- 기능 요구사항은 사용자가 수행할 수 있는 행동과 시스템이 제공해야 하는 기능을 정의한다.
- 비기능 요구사항은 보안, 성능, 신뢰성, 정합성, 운영성, 확장성처럼 기능의 품질 기준을 정의한다.
- 예외/제약사항은 실패 조건, 금지 조건, 정책상 제한을 정의한다.
