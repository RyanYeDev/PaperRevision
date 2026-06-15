# CLAUDE.md

必须使用中文回复

## 项目架构

PaperRevision 是一个基于 DDD（领域驱动设计）架构的智能论文返修平台。

### 后端 (Java/Spring Boot)
- **Domain 层**: 核心业务逻辑 - paper, revision, rag, agent, workflow, tool, evaluation, trace, llm, user
- **Application 层**: 业务流程编排 - AppService, Assembler, DTO
- **Infrastructure 层**: 技术实现 - config, utils, storage, mcp, rag
- **Interface 层**: API 控制器 - portal(用户端), admin(管理端)

### 前端 (Next.js/TypeScript)
- **Pages**: app/ 目录下的路由页面
- **Lib**: API 服务客户端
- **样式**: Tailwind CSS

### DDD 分层依赖原则（严格遵守）
- ✅ Infrastructure → Domain
- ✅ Application → Domain / Infrastructure
- ❌ Infrastructure → Application
- ❌ Domain → Infrastructure / Application

## 开发命令

```bash
# 后端
cd paper-revision-backend
./mvnw spring-boot:run          # 启动
./mvnw clean compile             # 编译
./mvnw test                      # 测试

# 前端
cd paper-revision-frontend
npm install --legacy-peer-deps   # 安装依赖
npm run dev                      # 启动开发服务器
npm run build                    # 构建

# Docker
docker-compose -f docker/docker-compose.yml up -d
```

## 代码规范
- NO Lombok - 使用标准 getter/setter
- MyBatis-Plus lambda 查询
- 统一使用 Result<T> 响应
- 构造函数注入
- 4 空格缩进
