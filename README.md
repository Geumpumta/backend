
# Geumpumta Backend
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/Geumpumta/backend)

공학 계열 대학생을 위한 집중 학습 시간 검증 및 랭킹 서비스 백엔드입니다.  

## 프로젝트 개요

Geumpumta는 대학생의 실제 학습 시간을 정확하게 측정하고  
개인 및 학과 단위의 랭킹 시스템을 제공하는 학습 관리 서비스입니다.

앱은 Wi-Fi SSID 기반 인증을 사용하여 부정 기록을 방지하고,  
타이머 기반 Heartbeat 구조를 통해 앱과 실시간으로 학습 시간을 동기화합니다.

## 주요 기능
- OAuth2 로그인 (Google/Kakao/Apple) + JWT 인증
- 캠퍼스 Wi-Fi 검증 기반 학습 세션 시작/하트비트/종료
- 학습 통계 (일/주/월/잔디형)
- 개인/학부 랭킹
- 게시판 기능
- 프로필/닉네임 검증, 이메일 인증
- 이미지 업로드 (Cloudinary)

## 기술 스택
- Java 21, Spring Boot 3.5.6
- Spring Security, OAuth2 Client, JWT
- Spring Data JPA, MySQL 8
- Redis
- Springdoc OpenAPI
- Spring Mail
- Cloudinary
- Actuator + Prometheus

## 아키텍처
<img width="1975" height="1093" alt="아키텍쳐 drawio" src="https://github.com/user-attachments/assets/395edabd-3b0b-48d4-8101-cd9f397dda82" />

## 패키지 구조
```
com.gpt.geumpumtabackend
├─ global
├─ user
│  ├─ api
│  ├─ controller
│  ├─ domain
│  ├─ repository
│  ├─ dto
│  └─ service
├─ token
├─ study
├─ statistics
├─ rank
├─ board
├─ image
└─ wifi
```
- 모놀리식 아키텍처 기반의 모듈형 패키지 구조 (도메인별 패키지 분리)
- 계층형 아키텍처(Controller/API → Service → Repository → Domain) 패턴
- DTO/Response 객체로 API 경계 분리

## 설정
- 프로파일: `local`, `dev`, `prod`
- 민감 정보는 GitHub 서브모듈로 별도 관리

## 팀원
|                                     채주혁                                      |                                      권오빈                                      |
|:----------------------------------------------------------------------------:|:-----------------------------------------------------------------------------:|
| <img src="https://github.com/user-attachments/assets/b4cc17f8-1bdf-4e9b-9b53-5e2396c26f63" width="80"/> | <img src="https://github.com/user-attachments/assets/4a9f4a31-a800-4a6c-b923-96099b393ab9" width="80"/> | 
|                     [@Juhye0k](https://github.com/Juhye0k)                     |                     [@kon28289](https://github.com/kon28289)                      |   
