#!/usr/bin/env bash
# ============================================================================
#  个人乐器配件与耗材管理册 —— 统一启动脚本
#  功能：
#    1. 加载根目录 .env，统一获取端口
#    2. 检测端口占用（若默认端口被占用，自动向后顺延）
#    3. 执行 docker compose up --build -d 全量构建启动
#    4. 等待前端 / 后端健康检查通过
#    5. 校验 http://127.0.0.1 和 http://localhost 访问一致性
#    6. 构建成功后自动打印访问地址
# ============================================================================
set -euo pipefail

# ------------ 基础颜色常量 ------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

info()    { echo -e "${GREEN}[INFO]${RESET}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
err()     { echo -e "${RED}[ERR ]${RESET}  $*" 1>&2; }
section() { echo -e "\n${BOLD}${CYAN}========== $* ==========${RESET}\n"; }

# ------------ 项目根目录 ------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ------------ 1. 加载 .env（严格检查存在） ------------
section "1/5 加载 .env 全局配置"
ENV_FILE="$SCRIPT_DIR/.env"
if [[ ! -f "$ENV_FILE" ]]; then
    err "未找到根目录 .env 文件，已中止。请确认 ${ENV_FILE} 是否存在。"
    exit 1
fi

# shellcheck disable=SC2046
export $(grep -Ev '^\s*#|^\s*$' "$ENV_FILE" | xargs)
info "已加载 .env：项目=${APP_NAME}, 前端=${FRONTEND_PORT}, 后端=${BACKEND_PORT}, MySQL=${MYSQL_PORT}, Redis=${REDIS_PORT}"

# ------------ 2. 端口占用检测（自动顺延） ------------
section "2/5 端口占用检测（自动顺延）"

check_port_occupied() {
    local port="$1"
    lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
}

resolve_port() {
    local var_name="$1"
    local desc="$2"
    local start_port="${!var_name}"
    local port="${start_port}"
    local max_port=$((start_port + 200))

    while check_port_occupied "${port}"; do
        warn "  ${desc}: 127.0.0.1:${port} 已占用，尝试 $((port + 1))"
        port=$((port + 1))
        if (( port > max_port )); then
            err "${desc} 从 ${start_port} 起连续 200 个端口均不可用。"
            exit 2
        fi
    done

    export "${var_name}=${port}"
    if [[ "${port}" != "${start_port}" ]]; then
        warn "  ${desc}: 已自动顺延为 127.0.0.1:${port}"
    else
        info "  ✅  ${desc}: 127.0.0.1:${port} 可用"
    fi
}

resolve_port "FRONTEND_PORT" "前端(Nginx)"
resolve_port "BACKEND_PORT"  "后端(SpringBoot)"
resolve_port "MYSQL_PORT"    "MySQL"
resolve_port "REDIS_PORT"    "Redis"

# ------------ 3. Docker Compose 构建与启动 ------------
section "3/5 执行 Docker Compose 构建 + 启动"

if ! command -v docker >/dev/null 2>&1; then
    err "未安装 docker，请先安装 Docker Desktop / Docker Engine。"
    exit 3
fi
if ! docker compose version >/dev/null 2>&1; then
    err "未检测到 docker compose 插件（v2）。请升级 Docker 至 20.x+。"
    exit 3
fi

info "镜像仓库：DOCKER_REGISTRY=${DOCKER_REGISTRY}"
info "执行: docker compose --env-file ${ENV_FILE} up --build -d"
docker compose --env-file "${ENV_FILE}" up --build -d

# ------------ 4. 健康检查轮询（最多 300 秒） ------------
section "4/5 等待容器健康检查通过"

wait_healthy() {
    local svc="$1"
    local timeout="${2:-300}"
    local elapsed=0
    local interval=5
    info "等待服务 ${svc} 就绪（超时：${timeout}s）..."
    while [[ $elapsed -lt $timeout ]]; do
        local state
        state="$(docker inspect --format='{{.State.Health.Status}}' "${APP_NAME}-${svc}" 2>/dev/null || echo 'not-ready')"
        if [[ "$state" == "healthy" ]]; then
            info "  ✅  ${svc}: healthy (耗时约 ${elapsed}s)"
            return 0
        fi
        sleep "${interval}"
        elapsed=$((elapsed + interval))
        # 每 20s 打一次进度，避免看起来卡住
        if (( elapsed % 25 == 0 )); then
            warn "  ⏳  仍在等待 ${svc}，当前状态：${state} ..."
        fi
    done
    err "  ❌  ${svc} 启动超时（>${timeout}s），请使用：docker logs ${APP_NAME}-${svc} 排查。"
    return 1
}

ALL_OK=1
wait_healthy "mysql"    90  || ALL_OK=0
wait_healthy "redis"    30  || ALL_OK=0
wait_healthy "backend"  180 || ALL_OK=0
wait_healthy "frontend" 30  || ALL_OK=0

if [[ ${ALL_OK} -ne 1 ]]; then
    err "并非所有容器均健康启动。请检查日志后重试。"
    exit 4
fi

# ------------ 5. 校验 127.0.0.1 与 localhost 访问一致性 ------------
section "5/5 访问地址一致性校验"

check_http() {
    local url="$1"
    local label="$2"
    # 最大尝试 3 次，间隔 2 秒（Nginx 刚启动可能 1-2 秒没准备好）
    for i in 1 2 3; do
        local body
        body="$(curl -sS --max-time 5 "$url" 2>/dev/null | head -n 20 || true)"
        if [[ -n "$body" ]]; then
            if echo "$body" | grep -qi "乐器配件管理\|instrument-accessory-manager\|个人乐器\|<!DOCTYPE html>"; then
                info "  ✅  ${label}（${url}） 正常返回，命中特征页"
                return 0
            fi
            # 部分情况下只有 html，没有标题也算成功（但至少有内容）
            if echo "$body" | grep -qi "<html\|<div\|<body"; then
                warn "  ⚠️  ${label}（${url}） 有响应但未匹配标题，返回摘要："
                echo "         ${body:0:300}..."
                return 0
            fi
        fi
        warn "  ⏳  第${i}次访问失败，2s 后重试 ..."
        sleep 2
    done
    err "  ❌  ${label}（${url}） 连续 3 次无法访问。"
    return 1
}

HTTP_OK=1
check_http "http://127.0.0.1:${FRONTEND_PORT}" "127.0.0.1"  || HTTP_OK=0
check_http "http://localhost:${FRONTEND_PORT}" "localhost"  || HTTP_OK=0

if [[ ${HTTP_OK} -ne 1 ]]; then
    err "前端访问一致性校验未通过，请检查容器日志。"
    exit 5
fi

# ============================================================
#  构建完成：打印访问地址（默认写 localhost）
# ============================================================
echo ""
echo -e "${BOLD}${GREEN}╔════════════════════════════════════════════════════════════════════╗${RESET}"
echo -e "${BOLD}${GREEN}║  🎸  个人乐器配件与耗材管理册 构建启动成功                          ║${RESET}"
echo -e "${BOLD}${GREEN}╚════════════════════════════════════════════════════════════════════╝${RESET}"
echo ""
echo -e "  🌐 前端访问地址（推荐）  :  ${BLUE}${BOLD}http://localhost:${FRONTEND_PORT}${RESET}"
echo -e "  🔗 前端（127.0.0.1 直连）:  ${CYAN}http://127.0.0.1:${FRONTEND_PORT}${RESET}"
echo ""
echo -e "  ⚙️  后端 API 地址         :  http://127.0.0.1:${BACKEND_PORT}/dashboard/stats"
echo -e "  🗄️  MySQL 本地连接        :  127.0.0.1:${MYSQL_PORT}  (库：${MYSQL_DATABASE}，用户：${MYSQL_USER})"
echo -e "  📦 Redis 本地连接         :  127.0.0.1:${REDIS_PORT}  (密码：${REDIS_PASSWORD})"
echo ""
echo -e "  📋 常用命令："
echo -e "       查看日志:  docker compose logs -f <frontend|backend|mysql|redis>"
echo -e "       停止服务:  docker compose down"
echo -e "       仅重启   :  docker compose restart"
echo -e "       重置数据:  docker compose down -v   (⚠️  会清掉 MySQL/Redis 数据卷)"
echo ""
info "全链路部署完成，祝您使用愉快 ~"
