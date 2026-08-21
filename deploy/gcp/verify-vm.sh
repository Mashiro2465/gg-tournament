#!/usr/bin/env bash
set -euo pipefail

readonly APP_DIR="/opt/gg-tournament"
readonly COMPOSE_FILE="${APP_DIR}/compose.prod.yaml"
readonly ENV_FILE="${APP_DIR}/.env.prod"

cd "${APP_DIR}"

sudo docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps
curl -fsS "http://localhost/actuator/health"
echo

placement_column="$(
  sudo docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" exec -T mysql \
    sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -Nse \
      "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '\''settlements'\'' AND COLUMN_NAME = '\''placement_rank'\'';"'
)"

if [[ "${placement_column}" != "placement_rank" ]]; then
  echo "settlements.placement_rank 컬럼을 확인할 수 없습니다." >&2
  exit 1
fi

echo "데이터베이스 스키마 확인 완료: settlements.placement_rank"
