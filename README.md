# 🏆 GG Tournament

> 누구나 게임 대회를 개설하고, 참가비를 걷고, 상금을 정산하는 e스포츠 커뮤니티 대회 플랫폼

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)](https://www.mysql.com)
[![Redis](https://img.shields.io/badge/Redis-7.0-red)](https://redis.io)

---

## 📌 프로젝트 소개

국내에는 소규모 게임 커뮤니티 대회를 직접 개설하고 참가비를 걷고 상금을 정산할 수 있는 플랫폼이 없습니다.
GG Tournament는 이 시장 공백을 겨냥해, 롤·오버워치 등 PC 게임 커뮤니티 대회를 누구나 손쉽게 운영할 수 있도록 만든 플랫폼입니다.

### 기획 배경
- 기존 LVUP.GG, 이스포츠 포털 등은 플랫폼이 직접 대회를 주최하는 구조 → **누구나 대회를 개설하는 구조가 없음**
- 참가비 결제 및 상금 자동 정산 기능을 갖춘 서비스 부재
- 해외의 Challonge, Battlefy 같은 서비스의 국내 게임 특화 버전

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| ORM | JPA + QueryDSL |
| Database | MySQL 8.0 |
| Cache | Redis (Redisson) |
| Auth | Spring Security + JWT |
| Social Login | Kakao OAuth2 |
| Payment | 토스페이먼츠 |
| Batch | Spring Batch |
| API Docs | Springdoc OpenAPI (Swagger) |
| Build | Gradle |

---

## 🏗 아키텍처

```
클라이언트
    │
    ▼
Spring Boot API (Spring Security + JWT)
    ├── Redis (분산 락 + JWT 블랙리스트)
    ├── MySQL (JPA + QueryDSL)
    ├── 토스페이먼츠 API
    └── Spring Batch (정산 스케줄러)
```

### 패키지 구조 (도메인 중심)
```
src/main/java/com/esports/platform/
├── domain/
│   ├── user/          # 회원/인증
│   ├── tournament/    # 대회 CRUD
│   ├── participant/   # 참가 신청
│   ├── payment/       # 결제
│   ├── bracket/       # 대진표
│   └── settlement/    # 정산
└── global/
    ├── auth/          # JWT 인프라
    ├── config/        # 설정
    └── exception/     # 공통 예외 처리
```

---

## 📊 ERD

| 테이블 | 설명 |
|--------|------|
| USERS | 회원 정보, 카카오 소셜 로그인 연동 |
| TOURNAMENTS | 대회 정보, 상금 구조(JSON), 상태 관리 |
| TOURNAMENT_PARTICIPANTS | 대회 참가자 연결, 참가 상태 관리 |
| PAYMENTS | 토스페이먼츠 결제 정보, 웹훅 처리 |
| MATCHES | 대진표, 라운드/경기번호 기반 구조 |
| SETTLEMENTS | 상금 정산 내역, 수수료 차감 |

---

## ⚙️ 핵심 구현 포인트

### 1. 참가 신청 동시성 제어 (Redis 분산 락)
정원이 있는 대회에 동시에 여러 요청이 들어올 경우 정원 초과를 방지하기 위해 Redisson 분산 락을 적용했습니다.

```
참가 신청 요청
  → Redisson tryLock(waitTime=3s, leaseTime=3s)
  → 정원 확인 → 참가 신청 + 결제 처리 (별도 트랜잭션)
  → unlock()
```

**핵심 설계 결정**: `ParticipantJoinExecutor`를 별도 빈으로 분리해 트랜잭션 커밋 후 락이 해제되도록 순서를 보장했습니다. 같은 클래스에 두면 self-invocation으로 `@Transactional`이 적용되지 않거나, 커밋 전에 락이 풀리는 경합이 발생할 수 있습니다.

---

### 2. 결제 연동 (토스페이먼츠)
```
클라이언트 결제 위젯
  → POST /api/payments/confirm (결제 승인)
  → 서버에서 금액 재검증 (클라이언트 금액 신뢰 금지)
  → 토스 API 호출
  → 웹훅 수신 → HMAC-SHA256 서명 검증 → 멱등 처리
```

**핵심 설계 결정**: `PaymentExecutor`를 별도 빈으로 분리해 외부 API 호출 중 DB 트랜잭션을 열어두지 않도록 했습니다. 조회/검증과 최종 반영을 짧은 트랜잭션 단위로 분리해 커넥션 점유 시간을 최소화했습니다.

---

### 3. 싱글 엘리미네이션 대진표 자동 생성
```
POST /api/tournaments/{id}/start
  → 결제 완료(CONFIRMED) 참가자만 대상
  → bracketSize = 참가자 수 이상인 최소 2의 거듭제곱
  → byeCount만큼 부전승 처리 → 즉시 다음 라운드 진출
  → 1라운드 경기 일괄 생성
```

---

### 4. 상금 정산 배치 (Spring Batch)
```
[매일 자정 스케줄]
Step 1 (Chunk): FINISHED 대회 조회 → 순위 계산 → PENDING Settlement 생성
Step 2 (Tasklet): PENDING → COMPLETED 일괄 전환
```

**핵심 설계 결정**: 2단계로 분리해 장애 발생 시 재시작 가능하도록 설계했습니다. 1단계에서 대회 단위로 원자적 커밋, 중단 후 재시작 시 남은 PENDING 건만 이어서 처리해 중복 정산을 방지합니다.

---

### 5. JWT 인증 + 로그아웃 처리
- Access Token 30분, Refresh Token 7일
- 로그아웃 시 Access Token을 Redis 블랙리스트에 등록 (TTL = 남은 만료 시간)
- 매 요청마다 블랙리스트 체크로 강제 로그아웃 구현

---

## 📡 API 명세

Swagger UI: `http://localhost:8080/swagger-ui.html`

| 도메인 | 엔드포인트 |
|--------|-----------|
| 인증 | POST /api/auth/signup, /login, /kakao, /refresh, /logout |
| 회원 | GET/PUT /api/users/me |
| 대회 | GET/POST /api/tournaments, GET/PUT/DELETE /api/tournaments/{id} |
| 참가 | GET/POST /api/tournaments/{id}/participants |
| 결제 | POST /api/payments/confirm, /webhook, /{id}/refund |
| 대진표 | GET /api/tournaments/{id}/bracket, PUT /api/matches/{id}/result |
| 정산 | GET /api/settlements/me, /api/tournaments/{id}/settlements |

---

## 🚀 로컬 실행 방법

### 1. 사전 준비
- Java 17
- Docker Desktop

### 2. 환경변수 설정
프로젝트 루트에 `.env` 파일 생성:
```
DB_URL=jdbc:mysql://localhost:3306/esports
DB_USERNAME=root
DB_PASSWORD=1234
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=localSecretKeyMustBeAtLeast32Characters
TOSS_CLIENT_KEY=test_ck_xxxxx
TOSS_SECRET_KEY=test_sk_xxxxx
KAKAO_CLIENT_ID=xxxxx
KAKAO_CLIENT_SECRET=xxxxx
```

### 3. Docker 실행 (MySQL + Redis)
```bash
docker-compose up -d
```

### 4. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 5. Swagger 접속
```
http://localhost:8080/swagger-ui.html
```

---
