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

## 코딩 컨벤션

### 네이밍
- 클래스: `PascalCase` (예: `TournamentService`)
- 메서드 / 변수: `camelCase` (예: `findByTournamentId`)
- 상수: `UPPER_SNAKE_CASE` (예: `MAX_PARTICIPANTS`)
- 테이블 / 컬럼: `snake_case` (예: `tournament_id`)
- URL: `kebab-case` (예: `/api/tournaments/{id}/participants`)

### 패키지 구조 (도메인 중심)
```
com.esports.
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

## 작업 규칙

### 코드 작성 원칙
- 한 번에 하나의 도메인만 작업
- 작업 순서: Entity → Repository → Service → Controller → DTO 순으로 작성
- 새 기능 시작 전 항상 PLAN.md의 해당 섹션 확인
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

## application.yml 구조

```yaml
spring:
  profiles:
    active: local  # local / prod

# 민감 정보는 환경변수로 관리
# JWT_SECRET, TOSS_SECRET_KEY, KAKAO_CLIENT_ID 등
# .env 파일 사용 (Git 제외)
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
```

---

## Claude Code에게 요청하는 방법

```
# 새 도메인 작업 시작
"PLAN.md 개발 순서 3번 대회 CRUD 시작해줘. CLAUDE.md 컨벤션 따라서 Entity부터 만들어줘"

# 특정 기능 구현
"CLAUDE.md 규칙대로 참가 신청 서비스에 Redis 분산 락 적용해줘"

# 코드 리뷰
"CLAUDE.md 컨벤션 기준으로 이 코드 리뷰해줘"

# 예외 처리 추가
"CLAUDE.md 예외 처리 규칙대로 ErrorCode에 결제 관련 에러 추가해줘"
```
