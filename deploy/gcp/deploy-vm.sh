#!/usr/bin/env bash
set -euo pipefail

readonly PROJECT_ID="gg-tournament-prod-2465"
readonly EXTERNAL_IP="34.47.65.133"
readonly REGISTRY="asia-northeast3-docker.pkg.dev"
readonly APP_DIR="/opt/gg-tournament"

sudo install -d -m 750 "${APP_DIR}/deploy/gcp"
sudo install -m 640 /tmp/compose.prod.yaml "${APP_DIR}/compose.prod.yaml"
sudo install -m 640 /tmp/Caddyfile "${APP_DIR}/deploy/gcp/Caddyfile"

if ! sudo test -f "${APP_DIR}/.env.prod"; then
  mysql_password="$(openssl rand -hex 24)"
  mysql_root_password="$(openssl rand -hex 24)"
  jwt_secret="$(openssl rand -hex 32)"

  sudo cp /tmp/.env.prod.example "${APP_DIR}/.env.prod"
  sudo sed -i "s#^APP_IMAGE=.*#APP_IMAGE=${REGISTRY}/${PROJECT_ID}/gg-tournament/api:latest#" "${APP_DIR}/.env.prod"
  sudo sed -i "s#^APP_DOMAIN=.*#APP_DOMAIN=http://${EXTERNAL_IP}#" "${APP_DIR}/.env.prod"
  sudo sed -i "s#^MYSQL_PASSWORD=.*#MYSQL_PASSWORD=${mysql_password}#" "${APP_DIR}/.env.prod"
  sudo sed -i "s#^MYSQL_ROOT_PASSWORD=.*#MYSQL_ROOT_PASSWORD=${mysql_root_password}#" "${APP_DIR}/.env.prod"
  sudo sed -i "s#^JWT_SECRET=.*#JWT_SECRET=${jwt_secret}#" "${APP_DIR}/.env.prod"
  sudo sed -i "s#^KAKAO_REDIRECT_URI=.*#KAKAO_REDIRECT_URI=http://${EXTERNAL_IP}/api/auth/kakao/callback#" "${APP_DIR}/.env.prod"
  sudo chmod 600 "${APP_DIR}/.env.prod"
fi

curl -fsS \
  -H "Metadata-Flavor: Google" \
  http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token \
  -o /tmp/registry-token.json

access_token="$(python3 -c 'import json; print(json.load(open("/tmp/registry-token.json"))["access_token"])')"
printf '%s' "${access_token}" | sudo docker login -u oauth2accesstoken --password-stdin "https://${REGISTRY}"
rm -f /tmp/registry-token.json

cd "${APP_DIR}"
sudo docker compose --env-file .env.prod -f compose.prod.yaml pull
sudo docker compose --env-file .env.prod -f compose.prod.yaml up -d

for attempt in $(seq 1 30); do
  if curl -fsS "http://localhost/actuator/health" >/dev/null; then
    sudo docker compose --env-file .env.prod -f compose.prod.yaml ps
    exit 0
  fi
  sleep 2
done

sudo docker compose --env-file .env.prod -f compose.prod.yaml logs --tail=100 app
echo "애플리케이션 헬스 체크가 제한 시간 안에 통과하지 못했습니다." >&2
exit 1
