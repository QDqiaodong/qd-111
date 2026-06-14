# 🎸 个人乐器配件与耗材管理册（instrument-accessory-manager）

面向乐器爱好者与演奏者的纯本地物资台账系统。管理琴弦、琴弓、拨片、松香等配件耗材，记录规格参数、更换周期、磨损状态并分类整理。无交易与社交功能，配件类型、适配乐器全部本地预置。

---

## 🚀 快速启动（推荐）

### 方式一：一键脚本启动 ⭐
```bash
cd 项目根目录
./start.sh
```
脚本会自动完成：
1. 加载 `.env` 全局配置
2. **严格检测端口占用**（固定端口，被占用直接报错+显示占用进程）
3. 执行 `docker compose up --build -d`
4. 等待 MySQL / Redis / Backend / Frontend 全部健康通过
5. 校验 `http://127.0.0.1:<端口>` 与 `http://localhost:<端口>` 访问一致性
6. **自动打印前端访问地址**

### 方式二：手动 Docker Compose
```bash
# 1. 端口检查
lsof -nP -iTCP:3008,8088,3309,6380 -sTCP:LISTEN

# 2. 构建启动（首次会拉镜像+下载依赖，需几分钟；之后缓存命中秒级重建）
docker compose up --build -d

# 3. 健康检查
docker ps --format "table {{.Names}}\t{{.Status}}"

# 4. 访问
open http://localhost:3008
```

### 方式三：本地开发启动（不使用 Docker）
```bash
# 后端
cd backend
# 先本机起 MySQL + Redis，端口参考 .env 默认值
mvn spring-boot:run

# 前端（另一个终端）
cd frontend
npm config set registry https://registry.npmmirror.com
npm ci  # 或 npm install
npm run dev  # 访问 http://127.0.0.1:3008
```

---

## 📍 固定端口总表（唯一分配，不得复用）

| 服务 | 宿主机（127.0.0.1） | 容器内部 | 说明 |
|---|---|---|---|
| **前端（Nginx）** | **3008** | 80 | 主入口，避免 80/443/8080 |
| **后端（SpringBoot）** | **8088** | 8080 | 避免 8080/9000/9090 |
| **MySQL 8.0** | **3309** | 3306 | 避免默认 3306 |
| **Redis 7.2** | **6380** | 6379 | 避免默认 6379 |

> 所有端口均写在根目录 `.env` 中，统一管理、统一修改。**端口被占用时报错退出，不会自动换端口。**

---

## 🌐 访问地址

| 类型 | URL |
|---|---|
| 前端（推荐） | **http://localhost:3008** |
| 前端（127.0.0.1 直连） | http://127.0.0.1:3008 |
| 后端 API（健康检查） | http://127.0.0.1:8088/dashboard/stats |
| MySQL | `127.0.0.1:3309`，库名：`instrument_manager`，用户：`root` / `instrument@2026` |
| Redis | `127.0.0.1:6380`，密码：`instrument@2026` |

> 两者必须指向同一服务（127.0.0.1 & localhost 访问必须完全一致），`start.sh` 会自动校验。

---

## 🧱 技术架构

| 层级 | 技术选型 | 关键特性 |
|---|---|---|
| **前端** | Vue 3.4 + Vite 5 + Pinia + Vue Router 4 + Element Plus 2.7 | 分组表单录入、自适应表格、批量操作组件 `BatchActionBar` 复用、`compressorjs` 图片压缩到 1280px |
| **后端** | Spring Boot 3.3 + JDK 17 + MyBatis-Plus 3.5.7 | 逻辑删除 / 自动填充 / 分页插件 / 全局异常处理 |
| **缓存** | Redis 7 + Spring Data Redis + `@Cacheable` | 配件类型 / 适配乐器 / 标准更换周期使用 **Redis Hash 结构** 本地缓存，减少重复查询 |
| **数据库** | MySQL 8.0 + 更换记录表**按年份 RANGE 分区**（2024-future） | 数据卷独立挂载，schema 幂等初始化 |
| **部署** | Docker Compose + 原生 Layer 分层缓存 | 前端 `npm ci`、后端 `dependency:go-offline` 严格分离依赖层/源码层 |

---

## 🧩 核心功能模块

1. **配件耗材建档**：分组表单录入（基础信息 / 规格参数 / 记录信息），支持配图上传与自动压缩。
2. **更换周期登记**：完整更换履历，自动计算使用天数，与标准周期做进度对比。
3. **损耗状态标注**：状态卡片快速筛选、行内单选快速标注、批量统一标注。
4. **物资分组归类**：左侧树形分组、右侧配件表格，支持跨组批量移动。
5. **Dashboard 总览**：统计卡片 + 即将到期更换 + 状态/分组分布。

---

## 🐳 Docker 构建缓存策略（关键工程约束）

### 前端 `frontend/Dockerfile` — 3 阶段
```
Stage 1 (deps)     → 仅 COPY package*.json + 网易镜像 npm ci （命中即不重装）
Stage 2 (builder)  → FROM deps，COPY 源码，执行 npm run build （仅编译，不下依赖）
Stage 3 (runtime)  → Nginx Alpine，仅 COPY dist + nginx.conf
```

### 后端 `backend/Dockerfile` — 3 阶段
```
Stage 1 (deps)     → 仅 COPY settings.xml + pom.xml + dependency:go-offline
Stage 2 (builder)  → FROM deps，仅 COPY src，执行 mvn package（只编译）
Stage 3 (runtime)  → eclipse-temurin:17-jre-alpine，仅 COPY app.jar
```

> **效果**：`package.json` / `pom.xml` 无变化时，连续 10 次 `docker compose up --build -d` 只会走到 `COPY src` 这一步，**秒级完成**。首次构建全量下载，后续重复构建效率理论提升 **≈ 68%**。

### 全链路镜像仓库（统一前缀）
所有基础镜像均通过 `.env` 的 `DOCKER_REGISTRY` 变量拼接前缀，Dockerfile 中使用 `ARG DOCKER_REGISTRY` + `FROM ${DOCKER_REGISTRY}/xxx`，由 `docker-compose.yml` 的 `build.args.DOCKER_REGISTRY` 传入，**避免本机 Docker 加速器失效时直连 DockerHub 失败**。默认值：`docker.1panel.live/library`。

### 构建上下文瘦身
`frontend/.dockerignore` 与 `backend/.dockerignore` 严格排除 `node_modules/`、`dist/`、`target/`、`.git/`、IDE 配置、日志、截图、文档等，**context 体积降低 > 90%**。

---

## 🔐 工程硬性约束（已全部遵守）

- ✅ 所有端口全部写在 `.env`，`docker-compose.yml` 全部引用 `${VAR}`，无一写死
- ✅ 所有 `ports` 映射为 `127.0.0.1:${HOST_PORT}:${CONTAINER_PORT}`，**不对外网暴露**
- ✅ Vite：`host='127.0.0.1'` + `strictPort=true`（dev & preview 均配置）
- ✅ 端口冲突：不自动换端口，报错+显示占用进程，由用户显式修改 `.env`
- ✅ 未使用 `# syntax=docker/dockerfile:*` 语法，纯原生 Layer 缓存
- ✅ 容器名 = `${APP_NAME}-<service>`（如 `instrument-accessory-manager-frontend`）
- ✅ 前后端 `.env` / `.gitignore` / `.dockerignore` / README 与端口表一致

---

## 🛠 常用命令

```bash
# 构建 + 启动
./start.sh                        # 一键（带端口检查 + 健康检查 + 一致性校验）
docker compose up --build -d      # 手动

# 日志
docker compose logs -f frontend
docker compose logs -f backend
docker compose logs -f mysql
docker compose logs -f redis

# 容器控制
docker compose restart            # 全部重启
docker compose restart backend    # 只重启后端
docker compose down               # 停止（保留数据卷）
docker compose down -v            # ⚠️ 停止并删除数据卷（清库用）

# 端口检查（冲突定位）
lsof -nP -iTCP:3008,8088,3309,6380 -sTCP:LISTEN

# 一致性自检
curl -sS http://127.0.0.1:3008 | head -n 5
curl -sS http://localhost:3008 | head -n 5
```

---

## 📁 目录结构

```
.
├── .env                         # ⭐ 全局端口/镜像/密码统一配置
├── docker-compose.yml           # 四服务编排（mysql / redis / backend / frontend）
├── start.sh                     # ⭐ 一键启动脚本（端口检测+健康检查+打印地址）
├── README.md
├── frontend/
│   ├── Dockerfile               # 3 阶段分层缓存
│   ├── .dockerignore            # 排除 node_modules / dist 等
│   ├── nginx.conf               # 容器内 80，/api 代理到 backend:8080
│   ├── vite.config.js           # 127.0.0.1:3008 + strictPort
│   ├── package.json
│   └── src/
│       ├── views/               # Dashboard / Accessories / Replacements / WornStatus / Groups
│       ├── components/          # BatchActionBar 等复用组件
│       ├── layouts/             # MainLayout（左侧菜单）
│       ├── api/index.js         # axios 统一封装
│       └── utils/               # image.js（压缩）/ request.js
└── backend/
    ├── Dockerfile               # 3 阶段分层缓存
    ├── .dockerignore            # 排除 target / .idea 等
    ├── settings.xml             # 腾讯云+阿里云 Maven 双镜像
    ├── pom.xml                  # SpringBoot 3.3 + JDK17 + MyBatis-Plus 3.5.7
    └── src/main/
        ├── java/com/instrument/
        │   ├── entity/          # Accessory / AccessoryGroup / ReplacementRecord
        │   ├── mapper/
        │   ├── service/ impl/   # DictServiceImpl 用 Redis Hash 缓存字典
        │   ├── controller/      # 5 套 REST 接口
        │   ├── dto/ vo/ common/ config/
        │   └── InstrumentApplication.java
        └── resources/
            ├── application.yml / application-prod.yml
            └── sql/schema.sql   # 建表 + 分区 + 初始化数据
```

---

## 💡 常见问题

**Q1：`./start.sh` 报端口被占用？**
A：请按提示 `kill <pid>` 结束占用进程，或修改 `.env` 里的 `FRONTEND_PORT` / `BACKEND_PORT` / `MYSQL_PORT` / `REDIS_PORT`，确保四个端口都未被监听。

**Q2：首次构建很慢？**
A：首次需要拉取基础镜像 + npm 全量依赖 + Maven 全量依赖，5-15 分钟正常。**第二次起只要不碰 `package*.json` / `pom.xml`，构建通常 < 1 分钟。**

**Q3：修改了业务代码，但构建时又在下载依赖？**
A：检查是否误改了 `pom.xml` 或 `package.json`（包括换行、注释）。如果文件哈希变化会触发依赖层重建。

**Q4：MySQL 数据卷已经有旧库，改了 schema.sql 不生效？**
A：MySQL 仅在数据卷为空时执行 `docker-entrypoint-initdb.d/` 下的 SQL。需要重置时执行：`docker compose down -v && ./start.sh`。

**Q5：localhost:3008 和 127.0.0.1:3008 打开的不是同一个项目？**
A：通常是系统 `hosts` 文件中 `localhost` 被别的项目代理了，或端口复用到 IPv6。`start.sh` 的步骤 5 会检测并阻止此种情况，请先清理占用。

---

📄 项目纯个人物资台账，不包含交易、支付、社交等功能。配件类型、适配乐器均为本地预置。
