# PaperRevision - 智能论文返修平台

基于 DDD（领域驱动设计）架构和 AI Agent 技术的智能论文返修平台。

## 核心技术栈

### 后端
- **语言**: Java 17
- **框架**: Spring Boot 3.2.3
- **架构**: DDD (Domain-Driven Design) 四层架构
- **ORM**: MyBatis-Plus 3.5.11
- **数据库**: PostgreSQL 16 + Milvus (向量数据库)
- **消息队列**: RabbitMQ
- **AI框架**: LangChain4j 1.0+
- **LLM**: DeepSeek / 豆包(Doubao)

### 前端
- **框架**: Next.js 15 + React 19
- **语言**: TypeScript
- **样式**: Tailwind CSS + Shadcn/ui

### 基础设施
- **容器化**: Docker + Docker Compose
- **CI/CD**: GitHub Actions

## 功能特性

- 论文PDF上传与解析
- 参考文献管理与RAG检索
- 返修意见输入与分析
- AI Agent自动执行返修工作流
- 修改建议生成与Diff对比
- Agent执行链路的全链路追踪与评估

## 快速开始

```bash
# 启动基础设施
docker-compose -f docker/docker-compose.yml up -d

# 启动后端
cd paper-revision-backend
./mvnw spring-boot:run

# 启动前端
cd paper-revision-frontend
npm install --legacy-peer-deps
npm run dev
```

## 项目结构

```
PaperRevision/
├── paper-revision-backend/     # Java后端 (DDD架构)
│   ├── domain/                 # 领域层
│   ├── application/            # 应用层
│   ├── infrastructure/         # 基础设施层
│   └── interfaces/             # 接口层
├── paper-revision-frontend/    # Next.js前端
├── docker/                     # Docker配置
└── docs/                       # 文档
```

## License

MIT
