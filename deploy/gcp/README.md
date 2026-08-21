# Google Compute Engine 배포

무료 체험 크레딧을 아끼기 위해 Compute Engine VM 한 대에서 API, MySQL, Redis, Caddy를
Docker Compose로 실행한다. 기본 리전은 서울(`asia-northeast3`)이다.

## 구성

- Compute Engine: `e2-small`, Ubuntu 24.04, 20GB Standard Persistent Disk
- Spring Boot API: 메모리 최대 768MB
- MySQL 8.0: 메모리 최대 640MB, 영구 볼륨
- Redis 7: 메모리 최대 192MB, AOF 영구 볼륨
- Caddy: HTTP/HTTPS 리버스 프록시

## 배포 파일

VM의 애플리케이션 디렉터리에 다음 파일이 필요하다.

- `compose.prod.yaml`
- `.env.prod` (`.env.prod.example`을 기반으로 작성하며 Git에 커밋하지 않음)
- `deploy/gcp/Caddyfile`

초기에는 `APP_DOMAIN=http://PUBLIC_IP`로 상태를 확인한다. 프론트엔드를 HTTPS로 배포하기
전에는 실제 도메인의 DNS A 레코드를 VM 고정 IP로 연결하고 `APP_DOMAIN=api.example.com`으로
바꿔 Caddy가 TLS 인증서를 발급받도록 한다.

현재 배포 대상은 다음과 같다.

- GCP 프로젝트: `gg-tournament-prod-2465`
- 리전/영역: `asia-northeast3` / `asia-northeast3-a`
- VM: `gg-tournament-vm`
- 고정 IP: `34.47.65.133`

## 실행

```bash
docker compose --env-file .env.prod -f compose.prod.yaml pull
docker compose --env-file .env.prod -f compose.prod.yaml up -d
docker compose --env-file .env.prod -f compose.prod.yaml ps
```

`deploy-vm.sh`는 기존 `.env.prod`가 있으면 비밀번호와 JWT 키를 덮어쓰지 않는다.
이미지를 갱신한 뒤 같은 스크립트를 다시 실행해도 기존 데이터와 운영 비밀값이 유지된다.

VM 안에서 배포 상태와 필수 데이터베이스 스키마를 함께 검증한다.

```bash
./deploy/gcp/verify-vm.sh
```

헬스체크:

```text
http://34.47.65.133/actuator/health
```

Swagger UI:

```text
http://34.47.65.133/swagger-ui.html
```

## 데이터 백업

MySQL 데이터는 Docker 볼륨에 저장되지만 VM 삭제를 대비해 별도 덤프가 필요하다.

```bash
docker compose --env-file .env.prod -f compose.prod.yaml exec mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --databases "$MYSQL_DATABASE"' > backup.sql
```

무료 체험 종료 전에는 백업 후 VM, 디스크, 고정 IP, Artifact Registry 이미지를 삭제한다.
