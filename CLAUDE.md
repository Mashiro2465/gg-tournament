# CLAUDE.md — Claude Code Rules

> **Language rule**: All responses, explanations, and comments must be written in Korean (한국어). Code identifiers (class names, method names, variables) follow the naming conventions below in English.

---

## Project Info

- **Name**: e-Sports Community Tournament Platform
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Build Tool**: Gradle
- **Design Doc**: See PLAN.md

---

## Local Dev Environment

- **OS**: Windows
- **JDK**: 17
- **IDE**: IntelliJ IDEA Community
- **Terminal**: PowerShell (IntelliJ built-in terminal)
- **Note**: Use `./gradlew` on Windows. Do not use Linux-only commands.

---

## Environment Variables (.env)

Create `.env` in project root (excluded from Git).
Referenced in `application-local.yml` as `${VAR:default}`.

```
# Database
DB_URL=jdbc:mysql://localhost:3306/esports
DB_USERNAME=root
DB_PASSWORD=1234

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=localSecretKeyMustBeAtLeast32Characters

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

## Docker Compose

Run MySQL + Redis locally via Docker.
Reference `docker-compose.yml` in project root:

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

```bash
docker-compose up -d    # start background
docker-compose down     # stop
docker-compose logs -f  # view logs
```

---

## Branch Strategy

### Structure
```
main               ← production-ready only
dev                ← integration branch
feat/auth          ← authentication
feat/tournament    ← tournament CRUD
feat/participant   ← participation
feat/payment       ← payment
feat/bracket       ← bracket
feat/settlement    ← settlement
```

### Flow
```
feat/{name} → (GitHub PR) → dev → (GitHub PR) → main
```

### Rules
- All feature work must start from a `feat/` branch
- Never commit directly to `main` or `dev`
- Always branch from latest `dev`
- Create GitHub PR when feat branch is complete; do not merge locally

### Branch creation
```bash
git checkout dev
git pull origin dev
git checkout -b feat/{name}
```

---

## Coding Conventions

### Naming
- Class: `PascalCase` (e.g. `TournamentService`)
- Method / Variable: `camelCase` (e.g. `findByTournamentId`)
- Constant: `UPPER_SNAKE_CASE` (e.g. `MAX_PARTICIPANTS`)
- Table / Column: `snake_case` (e.g. `tournament_id`)
- URL: `kebab-case` (e.g. `/api/tournaments/{id}/participants`)

### Package Structure (domain-driven)
```
com.esports.platform.
├── domain.{domain}.entity
├── domain.{domain}.repository
├── domain.{domain}.service
├── domain.{domain}.controller
├── domain.{domain}.dto
└── global.{shared}
```

### DTO Rules
- Request: `{Action}Request` (e.g. `CreateTournamentRequest`)
- Response: `{Subject}Response` (e.g. `TournamentDetailResponse`)
- Use `record` or `@Getter + @Builder`
- Never expose Entity beyond Service layer — always convert to DTO

### Entity Rules
- All entities extend `BaseTimeEntity` (`createdAt`, `updatedAt` auto-managed)
- No setters — use business methods for state changes
- Define relationship convenience methods inside the entity

---

## ⛔ Forbidden

### Code
- No `@Setter` or setter methods on Entity classes
- Never return Entity directly from Controller — always use DTO
- No business logic in Controller — belongs in Service
- No `System.out.println` — use `@Slf4j` + `log.info()`
- Never trust client-side payment amount — always re-validate on server

### Git
- No direct commits to `main`
- No direct commits to `dev`
- All feature work must be on `feat/` branches only

### Security
- Never commit `.env` to Git
- Never hardcode secrets or passwords in source code
- Never allow access to authenticated APIs without JWT verification

---

## Work Rules

### Order of implementation
1. Entity → Repository → Service → Controller → DTO
2. One domain at a time
3. Always check the relevant section in PLAN.md before starting a new feature
4. Always create a `feat/` branch before writing any code

### Exception Handling
- All errors managed via `ErrorCode` enum
- Single custom exception class: `BusinessException(ErrorCode)`
- Handled globally by `GlobalExceptionHandler`
- Error messages written in Korean

```java
throw new BusinessException(ErrorCode.TOURNAMENT_NOT_FOUND);
```

### API Response Format
Success:
```json
{
  "success": true,
  "data": {},
  "message": "요청이 처리되었습니다"
}
```
Failure:
```json
{
  "success": false,
  "code": "TOURNAMENT_NOT_FOUND",
  "message": "대회를 찾을 수 없습니다"
}
```

### Security Rules
- Use `@AuthenticationPrincipal UserPrincipal user` for authenticated APIs
- Verify host ownership before tournament update/delete
- Passwords: BCrypt
- JWT: Access Token 30min, Refresh Token 7 days
- Toss webhook: no JWT — verify via `X-Toss-Signature` header

### Redis Rules
- Distributed lock key: `lock:tournament:{tournamentId}`
- Lock timeout: 3 seconds
- JWT blacklist key: `blacklist:token:{token}`
- Blacklist TTL: remaining Access Token expiry time

### Payment Rules
- Always re-validate payment amount on server side
- `orderId` format: `TOURNAMENT_{tournamentId}_{userId}_{timestamp}`
- Webhook processing must be idempotent (prevent duplicate handling)

---

## Test Rules

- Unit tests required for Service layer (Mockito)
- Slice tests for Repository layer (`@DataJpaTest`)
- Test class name: `{ClassName}Test`
- Test method name: `{method}_{scenario}_{expectedResult}` (Korean allowed)

```java
@Test
void 참가신청_정원초과시_예외발생() { }
```

---

## Git Commit Format

```
feat: 대회 생성 API 구현
fix: 참가 신청 동시성 버그 수정
refactor: 결제 서비스 로직 분리
test: 대진표 생성 단위 테스트 추가
docs: PLAN.md 업데이트
chore: application.yml 설정 추가
```

---

## Common Commands

```bash
# Run locally
./gradlew bootRun

# Run tests
./gradlew test

# Build
./gradlew build

# Start Docker (MySQL + Redis)
docker-compose up -d

# Create branch
git checkout dev
git checkout -b feat/{name}

# Push feat branch and open PR
git push origin feat/{name}
# Then create PR on GitHub: feat/{name} → dev
```

---

## How to prompt Claude Code

```
# Start new domain
"PLAN.md 개발 순서 4번 참가 신청 시작해줘. feat/participant 브랜치 만들고 Entity부터."

# Specific feature
"CLAUDE.md Redis 규칙대로 분산 락 적용해줘."

# Code review
"CLAUDE.md 컨벤션 기준으로 이 코드 리뷰해줘."

# After feat branch complete
"feat/participant 작업 완료. origin에 push하고 CLAUDE.md 진행 상태 업데이트해줘."
```

---

## Current Progress

Update this section by asking: "CLAUDE.md 진행 상태 업데이트해줘"

### Done
- [x] Project setup (build.gradle, application.yml)
- [x] Package structure
- [x] Global classes (BaseTimeEntity, ApiResponse, ErrorCode, BusinessException, GlobalExceptionHandler)
- [x] Auth domain (feat/auth → dev merged)
  - JWT infra (JwtProvider, JwtAuthenticationFilter, UserPrincipal, TokenBlacklistService, SecurityConfig)
  - AuthController (signup/login/token-refresh/logout), UserController (GET/PUT /api/users/me)
  - Kakao login (`POST /api/auth/kakao`) — deferred
- [x] Tournament CRUD (feat/tournament → dev merged)
  - Tournament entity, TournamentRepository (QueryDSL dynamic search)
  - TournamentService (host validation, status transition), TournamentController
  - SecurityConfig: GET /api/tournaments/** permit all
- [x] Participation (feat/participant)
  - TournamentParticipant entity (unique constraint on tournament_id+user_id), TournamentParticipantRepository
  - TournamentParticipantService with Redis distributed lock (Redisson, `lock:tournament:{tournamentId}`, 3s wait/lease) for join, plain cancel
  - TournamentParticipantController (GET participants, POST join, DELETE /me cancel)
  - Refund on cancel is deferred — depends on Payment domain

### In Progress
- [ ] Payment (feat/payment)

### Pending
- [ ] Bracket (feat/bracket)
- [ ] Settlement (feat/settlement)
- [ ] Deployment