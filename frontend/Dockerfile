# 注意：禁止使用 # syntax=docker/dockerfile:xxx 语法
# 使用 Docker 原生分层缓存，确保 package*.json 无变更时不重新安装依赖

# ============================================================
# Stage 1: 依赖层（仅 package.json / package-lock.json 变更时触发）
# ============================================================
ARG DOCKER_REGISTRY
FROM ${DOCKER_REGISTRY}/node:20-alpine AS deps

WORKDIR /app

# 1. 仅拷贝依赖描述文件，触发分层缓存断点
#    此层内容变化（版本号/锁文件变更）时，后续 npm ci 才会重跑
COPY package.json package-lock.json ./

# 2. 使用网易 npm 国内镜像（npmmirror.com） + npm ci 精准锁版本安装
#    - ci 比 install 速度更快、更稳定，且严格遵循 lockfile
#    - 永久写入 npmrc，后续即便在本 stage 重新执行也不会再拉外网
RUN npm config set registry https://registry.npmmirror.com \
    && npm ci --no-audit --no-fund --ignore-scripts=false

# ============================================================
# Stage 2: 构建层（仅源文件变更时触发，复用 deps 层的 node_modules）
# ============================================================
FROM deps AS builder

WORKDIR /app

# 3. 依赖层已完成，只需要再 copy 源码进来执行 build
#    —— 只要 package*.json 没变，直接命中 deps 层缓存，不再跑 npm ci
COPY . .

# 4. 构建产物，保持 registry 仍是国内镜像（Vite/构建工具如果有额外子进程拉包也能走内网）
RUN npm config set registry https://registry.npmmirror.com \
    && npm run build

# ============================================================
# Stage 3: 运行层（Nginx 镜像，仅拷贝 dist + 配置）
# ============================================================
FROM ${DOCKER_REGISTRY}/nginx:1.27-alpine AS runtime

# 5. 容器内 Nginx 统一监听 80，宿主机映射由 docker-compose 管理
EXPOSE 80

# 6. 拷贝构建产物与 Nginx 配置
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

RUN chmod -R 755 /usr/share/nginx/html

CMD ["nginx", "-g", "daemon off;"]
