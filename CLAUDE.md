# CLAUDE.md — Claude Code 작업 규칙

이 파일은 Claude Code가 프로젝트 작업 시 항상 따라야 할 규칙과 컨벤션을 정의합니다.
작업 시작 전 반드시 이 파일과 PLAN.md를 함께 참조하세요.

---

## 프로젝트 기본 정보

- **프로젝트명**: e스포츠 커뮤니티 대회 플랫폼
- **언어**: Java 17
- **프레임워크**: Spring Boot 3.x
- **빌드 도구**: Gradle
- **설계 문서**: PLAN.md 참조

---

## 로컬 개발 환경

- **OS**: Windows
- **JDK**: 17
- **IDE**: IntelliJ IDEA Community
- **터미널**: PowerShell (IntelliJ 내장 터미널)
- **명령어 주의**: Windows 환경이므로 `./gradlew` 사용, Linux 명령어 사용 금지

---

## 환경변수 목록 (.env)

로컬 개발 시 프로젝트 루트에 `.env` 파일 생성 (Git 제외).
application-local.yml에서 `${VAR:default}` 형식으로 참조.

```
# Database
DB_URL=jdbc:mysql://localhost:3306/esports
DB_USERNAME=root
DB_PASSWORD=1234

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=로컬테스트용시크릿키최소32자이상이어야합니다

# Toss Payments
TOSS_CLIENT_KEY=test_ck_xxxxx
TOSS_SECRET_KEY=test_sk_xxxxx

# Kakao OAuth2
KAKAO_CLIENT_ID=xxxxx
KAKAO_CLIENT_SECRET=xxxxx
KAKAO_REDIRECT_URI=http://localhost:8080/api/auth/kakao

# AWS S3
AWS_ACCESS_KEY=xxxxx
AWS_SECRET_KEY=xxxxx
S3_BUCKET_NAME=esports-platform-bucket
S3_REGION=ap-northeast-2
```

---

## Docker Compose 설정

로컬 개발 시 MySQL + Redis는 Docker로 실행.
프로젝트 루트의 `docker-compose.yml` 기준:

```yaml
services:
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: 1234
      MYSQL_DATABASE: esports
    volumes:
      - mysql-data:/var/lib/mysql

  redis:
    image: redis:7.0
    ports:
      - "6379:6379"

volumes:
  mysql-data:
```

실행 명령어:
```bash
docker-compose up -d    # 백그라운드 실행
docker-compose down     # 종료
docker-compose logs -f  # 로그 확인
```

---

## 브랜치 전략

### 브랜치 구조
```
main               ← 최종 완성본 (항상 실행 가능한 상태 유지)
dev                ← 개발 통합 브랜치
feat/auth          ← 회원/인증
feat/tournament    ← 대회 CRUD
feat/participant   ← 참가 신청
feat/payment       ← 결제
feat/bracket       ← 대진표
feat/settlement    ← 정산
```

### 브랜치 흐름
```
feat/{기능} → dev → main
```

### 규칙
- 모든 기능 개발은 반드시 feat 브랜치에서 시작
- feat → dev 병합 전 기능이 정상 동작하는지 확인
- main은 배포 가능한 상태일 때만 병합
- 새 기능 시작 시 항상 dev에서 브랜치 생성

### 브랜치 생성 명령어
```bash
git checkout dev
git pull origin dev
git checkout -b feat/{기능명}
```

---

## 코딩 컨벤션

### 네이밍
- 클래스: `PascalCase` (예: `TournamentService`)
- 메서드 / 변수: `camelCase` (예: `findByTournamentId`)
- 상수: `UPPER_SNAKE_CASE` (예: `MAX_PARTICIPANTS`)
- 테이블 / 컬럼: `snake_case` (예: `tournament_id`)
- URL: `kebab-case` (예: `/api/tournaments/{id}/participants`)

### 패키지 구조 (도메인 중심)
```
com.esports.platform.
├── domain.{도메인}.entity
├── domain.{도메인}.repository
├── domain.{도메인}.service
├── domain.{도메인}.controller
├── domain.{도메인}.dto
└── global.{공통모듈}
```

### DTO 규칙
- 요청: `{기능}Request` (예: `CreateTournamentRequest`)
- 응답: `{기능}Response` (예: `TournamentDetailResponse`)
- DTO는 record 또는 @Getter + @Builder 사용
- Entity를 Controller까지 노출하지 않음 (반드시 DTO 변환)

### Entity 규칙
- 모든 Entity는 `BaseTimeEntity` 상속 (`createdAt`, `updatedAt` 자동 관리)
- Setter 사용 금지 → 비즈니스 메서드로 상태 변경
- 연관관계 편의 메서드 Entity 내부에 작성

---

## ⛔ 절대 금지 사항

Claude Code는 아래 사항을 절대 하지 않는다.

### 코드 관련
- Entity에 `@Setter` 또는 `setter` 메서드 작성 금지
- Controller에서 Entity 직접 반환 금지 (반드시 DTO 변환)
- 비즈니스 로직을 Controller에 작성 금지 (Service에서 처리)
- `System.out.println` 사용 금지 (로그는 `@Slf4j` + `log.info()` 사용)
- 결제 금액을 클라이언트 값 그대로 신뢰 금지 (서버에서 재검증 필수)

### Git 관련
- `main` 브랜치에 직접 커밋 금지
- `dev` 브랜치에 직접 커밋 금지
- 기능 개발은 반드시 `feat/` 브랜치에서만 진행

### 보안 관련
- `.env` 파일 Git 커밋 금지
- 시크릿 키, 비밀번호를 코드에 하드코딩 금지
- JWT 검증 없이 인증 필요 API 접근 허용 금지

---

## 작업 규칙

### 코드 작성 원칙
- 한 번에 하나의 도메인만 작업
- 작업 순서: Entity → Repository → Service → Controller → DTO 순으로 작성
- 새 기능 시작 전 항상 PLAN.md의 해당 섹션 확인
- 새 기능 시작 시 항상 feat 브랜치 먼저 생성 후 작업
- 기존 코드 수정 시 영향 범위 먼저 파악 후 진행

### 예외 처리
- 모든 예외는 `ErrorCode` enum으로 관리
- 커스텀 예외: `BusinessException(ErrorCode)` 단일 클래스 사용
- `GlobalExceptionHandler`에서 일괄 처리
- 예외 메시지는 한국어로 작성

```java
// 예시
throw new BusinessException(ErrorCode.TOURNAMENT_NOT_FOUND);
```

### API 응답 형식
모든 API는 아래 형식으로 통일:
```json
{
  "success": true,
  "data": { },
  "message": "요청이 처리되었습니다"
}
```
실패 시:
```json
{
  "success": false,
  "code": "TOURNAMENT_NOT_FOUND",
  "message": "대회를 찾을 수 없습니다"
}
```

### 보안 규칙
- 인증이 필요한 API는 `@AuthenticationPrincipal UserPrincipal user` 사용
- 권한 체크: 대회 수정/삭제는 주최자 본인 확인 필수
- 비밀번호: BCrypt 암호화
- JWT: Access Token 30분, Refresh Token 7일
- 토스 웹훅: JWT 없이 `X-Toss-Signature` 헤더 검증

### Redis 사용 규칙
- 분산 락 키: `lock:tournament:{tournamentId}`
- 락 타임아웃: 3초
- JWT 블랙리스트 키: `blacklist:token:{token}`
- 블랙리스트 TTL: Access Token 남은 만료 시간

### 결제 규칙
- 결제 금액은 항상 서버에서 재검증 (클라이언트 금액 신뢰 금지)
- `orderId` = `TOURNAMENT_{tournamentId}_{userId}_{timestamp}` 형식
- 웹훅 처리는 멱등성 보장 (중복 처리 방지)

---

## 테스트 규칙

- Service 레이어 단위 테스트 필수 (Mockito 사용)
- Repository 레이어 슬라이스 테스트 (@DataJpaTest)
- 테스트 클래스명: `{클래스명}Test`
- 테스트 메서드명: `{메서드명}_{시나리오}_{기대결과}` (한국어 가능)

```java
// 예시
@Test
void 참가신청_정원초과시_예외발생() { }
```

---

## Git 커밋 규칙

```
feat: 대회 생성 API 구현
fix: 참가 신청 동시성 버그 수정
refactor: 결제 서비스 로직 분리
test: 대진표 생성 단위 테스트 추가
docs: PLAN.md 업데이트
chore: application.yml 설정 추가
```

---

## 자주 쓰는 명령어

```bash
# 로컬 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# 빌드
./gradlew build

# Docker 실행 (MySQL + Redis)
docker-compose up -d

# 브랜치 생성
git checkout dev
git checkout -b feat/{기능명}

# 작업 완료 후 dev에 병합
git checkout dev
git merge feat/{기능명}
```

---

## Claude Code에게 요청하는 방법

```
# 새 도메인 작업 시작 (브랜치 생성부터)
"PLAN.md 개발 순서 2번 회원/인증 시작해줘. feat/auth 브랜치 만들고 CLAUDE.md 컨벤션 따라서 Entity부터 만들어줘"

# 특정 기능 구현
"CLAUDE.md 규칙대로 참가 신청 서비스에 Redis 분산 락 적용해줘"

# 코드 리뷰
"CLAUDE.md 컨벤션 기준으로 이 코드 리뷰해줘"

# 예외 처리 추가
"CLAUDE.md 예외 처리 규칙대로 ErrorCode에 결제 관련 에러 추가해줘"

# 브랜치 작업 완료
"feat/auth 작업 완료됐어. dev에 병합하고 다음 브랜치 feat/tournament 만들어줘"

# 금지 사항 점검
"CLAUDE.md 금지 사항 기준으로 현재 코드 점검해줘"
```

---

## 현재 진행 상태

작업 완료 시마다 "CLAUDE.md 진행 상태 업데이트해줘" 로 갱신.

### 완료
- [x] 프로젝트 세팅 (build.gradle, application.yml)
- [x] 패키지 구조 생성
- [x] 공통 클래스 (BaseTimeEntity, ApiResponse, ErrorCode, BusinessException, GlobalExceptionHandler)

### 진행 중
- [ ] 개발 순서 2번: 회원/인증 (feat/auth)

### 대기
- [ ] 개발 순서 3번: 대회 CRUD (feat/tournament)
- [ ] 개발 순서 4번: 참가 신청 (feat/participant)
- [ ] 개발 순서 5번: 결제 (feat/payment)
- [ ] 개발 순서 6번: 대진표 (feat/bracket)
- [ ] 개발 순서 7번: 정산 (feat/settlement)
- [ ] 개발 순서 8번: 배포