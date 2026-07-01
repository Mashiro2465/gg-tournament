# e스포츠 커뮤니티 대회 플랫폼

## 프로젝트 개요

누구나 소규모 게임 대회를 개설하고, 참가자를 모집하고, 참가비를 걷고, 대진표를 관리하고, 우승자에게 상금을 정산하는 플랫폼.

- **목적**: Java 백엔드 포트폴리오
- **도메인**: e스포츠 / 게임 커뮤니티
- **차별점**: 국내에 "누구나 대회를 개설하고 참가비를 걷고 상금을 정산"하는 플랫폼이 없음

---

## 기술 스택

| 레이어 | 기술 |
|--------|------|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.x |
| ORM | JPA + QueryDSL |
| 데이터베이스 | MySQL 8.x |
| 캐시 | Redis |
| 인증 | Spring Security + JWT |
| 소셜 로그인 | 카카오 OAuth2 |
| 결제 | 토스페이먼츠 |
| 배치 | Spring Batch |
| 파일 저장 | AWS S3 |
| 실시간 | SSE (Server-Sent Events) |
| 인프라 | AWS EC2, Docker, GitHub Actions, Route 53 |
| 빌드 도구 | Gradle |

---

## 사용자 역할

| 역할 | 설명 |
|------|------|
| `USER` | 일반 회원. 대회 참가, 결제, 결과 조회 |
| `HOST` | 대회 주최자. 대회 생성, 대진표 관리, 결과 입력 |
| `ADMIN` | 플랫폼 관리자. 수수료 관리, 신고 처리, 정산 승인 |

---

## MVP 기능 범위

### 회원
- [ ] 회원가입 / 로그인 (JWT)
- [ ] 카카오 소셜 로그인
- [ ] 프로필 관리
- [ ] 내 대회 목록 조회
- [ ] 내 결제 내역 조회

### 대회
- [ ] 대회 개설 (종목, 방식, 인원, 참가비, 상금 구조 설정)
- [ ] 대회 목록 조회 / 검색 / 필터 (QueryDSL 동적 쿼리)
- [ ] 대회 상세 조회
- [ ] 대회 수정 / 취소

### 참가 신청
- [ ] 참가 신청 + 참가비 결제 (Redis 분산 락으로 동시성 제어)
- [ ] 참가 취소 + 환불 처리
- [ ] 참가자 목록 조회

### 결제
- [ ] 토스페이먼츠 단건 결제
- [ ] 웹훅 수신 및 처리 (결제 확정 / 실패)
- [ ] 환불 처리
- [ ] JWT 블랙리스트 (Redis)

### 대진표
- [ ] 참가 마감 후 대진표 자동 생성 (싱글 엘리미네이션)
- [ ] 경기 결과 입력
- [ ] 다음 라운드 자동 진행
- [ ] SSE로 대진표 실시간 업데이트

### 정산
- [ ] 대회 종료 후 상금 자동 정산 (Spring Batch)
- [ ] 플랫폼 수수료 차감 (기본 10%)
- [ ] 정산 내역 조회

---

## 데이터베이스 설계 (ERD)

### USERS
```sql
id            BIGINT PK
email         VARCHAR(255) UNIQUE NOT NULL
password      VARCHAR(255)
nickname      VARCHAR(50) NOT NULL
profile_image VARCHAR(500)
role          ENUM('USER', 'ADMIN') DEFAULT 'USER'
kakao_id      VARCHAR(100)
created_at    DATETIME NOT NULL
```

### TOURNAMENTS
```sql
id                    BIGINT PK
host_id               BIGINT FK → USERS.id
title                 VARCHAR(200) NOT NULL
game_type             VARCHAR(50) NOT NULL  -- 롤, 오버워치, 발로란트 등
format                ENUM('SINGLE_ELIMINATION', 'DOUBLE_ELIMINATION', 'LEAGUE')
max_participants      INT NOT NULL
current_participants  INT DEFAULT 0
entry_fee             DECIMAL(10,2) DEFAULT 0
prize_pool            DECIMAL(10,2) DEFAULT 0
prize_structure       JSON  -- {"1st": 50, "2nd": 30, "3rd": 20}
status                ENUM('RECRUITING', 'CLOSED', 'IN_PROGRESS', 'FINISHED', 'CANCELLED')
registration_deadline DATETIME NOT NULL
start_at              DATETIME NOT NULL
created_at            DATETIME NOT NULL
```

### TOURNAMENT_PARTICIPANTS
```sql
id             BIGINT PK
tournament_id  BIGINT FK → TOURNAMENTS.id
user_id        BIGINT FK → USERS.id
status         ENUM('PENDING', 'CONFIRMED', 'CANCELLED')
joined_at      DATETIME NOT NULL
```

### PAYMENTS
```sql
id             BIGINT PK
user_id        BIGINT FK → USERS.id
tournament_id  BIGINT FK → TOURNAMENTS.id
order_id       VARCHAR(100) UNIQUE NOT NULL  -- 토스 주문 ID
payment_key    VARCHAR(200)                  -- 토스 결제 키
amount         DECIMAL(10,2) NOT NULL
status         ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'REFUNDED')
paid_at        DATETIME
refunded_at    DATETIME
```

### MATCHES
```sql
id               BIGINT PK
tournament_id    BIGINT FK → TOURNAMENTS.id
round            INT NOT NULL      -- 1라운드, 2라운드...
match_number     INT NOT NULL      -- 라운드 내 경기 번호
participant1_id  BIGINT FK → TOURNAMENT_PARTICIPANTS.id
participant2_id  BIGINT FK → TOURNAMENT_PARTICIPANTS.id
winner_id        BIGINT FK → TOURNAMENT_PARTICIPANTS.id
status           ENUM('SCHEDULED', 'IN_PROGRESS', 'FINISHED', 'BYE')
played_at        DATETIME
```

### SETTLEMENTS
```sql
id             BIGINT PK
tournament_id  BIGINT FK → TOURNAMENTS.id
user_id        BIGINT FK → USERS.id
rank           INT NOT NULL
prize_amount   DECIMAL(10,2) NOT NULL
platform_fee   DECIMAL(10,2) NOT NULL
status         ENUM('PENDING', 'COMPLETED', 'FAILED')
settled_at     DATETIME
```

---

## API 설계

### 인증
```
POST /api/auth/signup          회원가입
POST /api/auth/login           로그인 (JWT 발급)
POST /api/auth/kakao           카카오 소셜 로그인
POST /api/auth/refresh         토큰 재발급
POST /api/auth/logout          로그아웃 [인증 필요]
```

### 회원
```
GET  /api/users/me                  내 정보 조회 [인증 필요]
PUT  /api/users/me                  내 정보 수정 [인증 필요]
GET  /api/users/me/tournaments      내 대회 목록 [인증 필요]
GET  /api/users/me/payments         내 결제 내역 [인증 필요]
```

### 대회
```
GET    /api/tournaments              대회 목록 조회 (필터/검색)
GET    /api/tournaments/{id}         대회 상세 조회
POST   /api/tournaments              대회 생성 [인증 필요]
PUT    /api/tournaments/{id}         대회 수정 [인증 필요]
DELETE /api/tournaments/{id}         대회 취소 [인증 필요]
POST   /api/tournaments/{id}/start   대회 시작 → 대진표 생성 [인증 필요]
```

### 참가 신청
```
GET    /api/tournaments/{id}/participants      참가자 목록
POST   /api/tournaments/{id}/participants      참가 신청 [인증 필요]
DELETE /api/tournaments/{id}/participants/me   참가 취소 [인증 필요]
```

### 결제
```
POST /api/payments/confirm       결제 승인 요청 [인증 필요]
POST /api/payments/webhook       토스 웹훅 수신 (인증 없음 - 서명 검증)
POST /api/payments/{id}/refund   환불 요청 [인증 필요]
GET  /api/payments/{id}          결제 상세 조회 [인증 필요]
```

### 대진표 / 경기
```
GET /api/tournaments/{id}/bracket   대진표 조회
PUT /api/matches/{id}/result        경기 결과 입력 [인증 필요]
```

### 정산
```
GET /api/settlements/me                      내 정산 내역 [인증 필요]
GET /api/tournaments/{id}/settlements        대회 정산 현황 [인증 필요]
```

---

## 핵심 구현 포인트

### 1. 참가 신청 동시성 제어 (Redis 분산 락)
```
참가 신청 요청
  → Redis Lock 획득 (tournament:{id}:lock)
  → 현재 참가 인원 확인
  → 정원 미달 시 참가 신청 + 결제 처리
  → Lock 해제
```
- 면접 포인트: "정원 초과 방지를 위해 Redis 분산 락 적용"

### 2. 결제 플로우 (토스페이먼츠)
```
클라이언트 결제 위젯 → 토스 서버 결제 요청
  → POST /api/payments/confirm (결제 승인)
  → 토스 웹훅 → POST /api/payments/webhook
  → 결제 확정 시 참가 상태 CONFIRMED 업데이트
```
- 웹훅은 JWT 없이 토스 서명(X-Toss-Signature) 검증으로 보안 처리

### 3. 대진표 자동 생성 (싱글 엘리미네이션)
```
POST /api/tournaments/{id}/start 호출
  → 참가자 셔플 (시드 배정)
  → 2^n 계산 → 부전승 처리
  → MATCHES 테이블에 1라운드 경기 일괄 생성
```

### 4. 상금 정산 배치 (Spring Batch)
```
[매일 자정 스케줄]
  → FINISHED 상태 대회 조회
  → 우승자 순위 확인
  → 수수료(10%) 차감 후 SETTLEMENTS 생성
  → 상태 COMPLETED 업데이트
```
- 면접 포인트: "API 대신 배치로 처리한 이유 → 트랜잭션 안정성, 재시작 가능"

### 5. SSE 실시간 대진표
```
GET /api/tournaments/{id}/bracket/stream (SSE 연결)
  → 경기 결과 입력 시 해당 대회 구독자에게 이벤트 발행
  → 클라이언트 대진표 자동 갱신
```

---

## 프로젝트 구조

```
src/main/java/com/esports/
├── domain/
│   ├── user/
│   │   ├── entity/User.java
│   │   ├── repository/UserRepository.java
│   │   ├── service/UserService.java
│   │   └── controller/UserController.java
│   ├── tournament/
│   ├── participant/
│   ├── payment/
│   ├── match/
│   └── settlement/
├── global/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── RedisConfig.java
│   │   └── BatchConfig.java
│   ├── auth/
│   │   ├── JwtProvider.java
│   │   └── JwtFilter.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── ErrorCode.java
└── EsportsApplication.java
```

---

## 개발 순서 (권장)

1. **프로젝트 세팅** — Spring Initializr, 의존성, application.yml
2. **회원/인증** — 회원가입, 로그인, JWT, 카카오 로그인
3. **대회 CRUD** — 대회 생성, 목록, 상세, 수정, 취소
4. **참가 신청** — 신청, 취소, Redis 분산 락
5. **결제** — 토스페이먼츠 연동, 웹훅 처리, 환불
6. **대진표** — 자동 생성, 결과 입력, SSE 실시간 업데이트
7. **정산** — Spring Batch 정산 배치
8. **배포** — Docker, GitHub Actions CI/CD, AWS EC2

---

## Claude Code 사용 팁

- 각 단계 시작 시 이 파일을 참조하도록 요청: `PLAN.md를 참고해서 [기능] 구현해줘`
- 새 기능 시작 전: `PLAN.md의 개발 순서 2번 회원/인증 구현 시작해줘`
- 막힐 때: `PLAN.md 기준으로 결제 플로우 다시 설명해줘`
