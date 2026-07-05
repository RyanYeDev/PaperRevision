# Agent 评估系统详解

> PaperRevision 智能论文返修平台的 Agent 评估子系统，基于 DDD 四层架构实现。  
> 覆盖轨迹分析、LLM 裁判、测试套件三大模块，对应业界标准的 Agent 评估知识体系。

---

## 目录

1. [架构总览](#一架构总览)
2. [四条数据通路](#二四条数据通路)
3. [8 大轨迹指标详解](#三8-大轨迹指标详解)
4. [LLM 裁判评分标准](#四llm-裁判评分标准)
5. [数据表关系](#五数据表关系)
6. [DDD 设计决策](#六ddd-设计决策)
7. [核心类清单](#七核心类清单)
8. [API 端点参考](#八api-端点参考)
9. [面试话术](#九面试话术)

---

## 一、架构总览

```
┌──────────────────────────────────────────────────────────────────────┐
│                         评估系统 DDD 架构                              │
├──────────────┬──────────────┬──────────────────────┬─────────────────┤
│  Interface   │  Application │       Domain         │  Infrastructure │
│              │              │                      │                 │
│ Evaluation   │ Evaluation   │ EvaluationDomain     │ LLMJudge        │
│ Controller   │ AppService   │ Service (编排)        │ ServiceImpl     │
│   ↓          │   ↓          │   ├─ Trajectory       │   ↓             │
│ POST /api/   │ DTO/Assembler│   │   Analyzer        │ LLMService      │
│ evaluation/* │ 转换          │   │   (8 指标计算)     │ .createChat     │
│              │              │   ├─ LLMJudgeService  │ Model()         │
│ TestCase     │              │   │   (Domain 接口)    │   ↓             │
│ Controller   │              │   │   ↓               │ DeepSeek/       │
│   ↓          │              │   │ LLMJudgeService   │ Doubao API      │
│ CRUD         │              │   │ Impl (Infra 实现)  │                 │
│              │              │   ├─ TestSuite        │                 │
│ TestSuite    │              │   │   Executor        │                 │
│ Controller   │              │   │   (批量执行)       │                 │
│   ↓          │              │   └─ EvaluationReport │                 │
│ run/reports  │              │       (聚合根)        │                 │
│              │              │                      │                 │
│ Revision     │              │ ┌─ Entity ──────────┐ │                 │
│ Controller   │              │ │ EvaluationEntity  │ │                 │
│ (入口)       │              │ │ TraceEntity       │ │                 │
│              │              │ │ TestCaseEntity    │ │                 │
│              │              │ │ TestSuiteEntity   │ │                 │
│              │              │ │ EvaluationReport  │ │                 │
│              │              │ │ EvalReportItem    │ │                 │
│              │              │ └───────────────────┘ │                 │
│              │              │ ┌─ Repository ───────┐ │                 │
│              │              │ │ 6 个 Mapper 接口    │ │                 │
│              │              │ └────────────────────┘ │                 │
│              │              │ ┌─ Value Object ─────┐ │                 │
│              │              │ │ TrajectoryEvalResult│                 │
│              │              │ │ LLMJudgeRequest    │ │                 │
│              │              │ │ LLMJudgeResult     │ │                 │
│              │              │ │ EvaluationRubric   │ │                 │
│              │              │ └────────────────────┘ │                 │
└──────────────┴──────────────┴──────────────────────┴─────────────────┘
```

---

## 二、四条数据通路

### 通路 1：返修执行 → 自动评估

这是**核心入口**，每次用户提交返修请求时自动触发完整的评估链路。

```
POST /api/revision/execute
  │
  ├── ① ExecutionTraceDomainService.recordStep()
  │     为每一步写一条记录到 agent_execution_traces 表
  │     字段包含: phase, stepType, durationMs, tokensUsed, status
  │
  ├── ② RevisionExecutor.executeRevision()
  │     逐条处理审稿意见，RAG 检索参考文献
  │
  ├── ③ EvaluationDomainService.evaluateRevision()  ★ 评估核心
  │     │
  │     ├── AgentExecutionTraceRepository.findBySessionId()
  │     │   从 DB 读取本次执行的全部 trace
  │     │
  │     ├── TrajectoryAnalyzer.analyze()
  │     │   计算 8 项轨迹指标 → TrajectoryEvalResult
  │     │
  │     ├── LLMJudgeService.evaluate()      ← Phase 2
  │     │   构建 LLMJudgeRequest → 调用 LLM → LLMJudgeResult
  │     │   失败时自动降级，不阻塞评估流程
  │     │
  │     ├── 加权融合
  │     │   LLM 裁判权重 65%，轨迹分析权重 35%
  │     │   降级模式：100% 轨迹分析
  │     │
  │     └── 持久化
  │         EvaluationEntity → evaluations 表
  │
  └── ④ 返回 response
       evaluation: { relevanceScore, faithfulnessScore, completenessScore,
                     formatScore, overallScore, grade, evaluatorType, feedback }
```

**代码位置**：`RevisionController.java:84-98`

### 通路 2：独立轨迹查询

绕过完整评估，直接对任意一次执行做轨迹分析。

```
GET /api/evaluation/trajectory/{sessionId}
  │
  ├── AgentExecutionTraceRepository.findBySessionId()
  ├── TrajectoryAnalyzer.analyze()
  └── 返回 12 项轨迹指标 JSON
```

**返回示例**：

```json
{
  "code": 200,
  "data": {
    "stepEfficiency": 0.67,
    "toolAccuracy": 0.83,
    "paramCorrectness": 0.75,
    "redundancyRate": 0.17,
    "totalDurationMs": 3200,
    "totalTokens": 1500,
    "successRate": 0.83,
    "totalModelCalls": 6,
    "totalSteps": 6,
    "successSteps": 5,
    "effectiveToolCalls": 5,
    "totalToolCalls": 6
  }
}
```

### 通路 3：LLM 裁判评估

利用用户已配置的 LLM（DeepSeek/Doubao）作为裁判，做语义层面的质量评估。

```
EvaluationDomainService.evaluateRevision()
  │
  ├── 构建 LLMJudgeRequest
  │    - originalText:   论文原文
  │    - revisedText:    修改后的文本
  │    - requirement:    审稿意见
  │    - referenceText:  参考文献上下文
  │    - rubricConfig:   评分标准描述
  │
  ├── LLMJudgeServiceImpl.evaluate()
  │    ├── 获取启用的 LLM 提供商 (LLMProviderDomainService)
  │    ├── buildJudgePrompt()
  │    │    包含: 5 维评分标准 + 幻觉检测任务 + 原文/修改/需求
  │    │    要求 LLM 返回纯 JSON
  │    ├── LLMService.createChatModel() → 调用 API
  │    └── parseJudgeResponse()
  │         提取 JSON → 1-5 分 → normalizeScore() → 0.0-1.0
  │
  └── 轨迹分 × 0.35 + LLM 裁判分 × 0.65 → 最终评分
```

**关键设计**：`LLMJudgeService` 接口在 Domain 层，实现在 Infrastructure 层，遵循**依赖倒置原则 (DIP)**。

### 通路 4：测试套件批量评估

定义测试用例集合，批量运行后生成聚合评估报告。

```
POST /api/evaluation/test-suites/{id}/run
  │
  ├── TestSuiteExecutor.executeSuite()
  │    ├── 获取套件 + 关联的用例列表
  │    ├── 创建 EvaluationReport (状态: IN_PROGRESS)
  │    ├── 逐用例执行:
  │    │    executeSingleCase()
  │    │    └── EvaluationDomainService.evaluateRevision()
  │    │    └── 生成 EvalReportItem → 持久化
  │    ├── report.addItem(item) × N
  │    └── report.computeAggregates()
  │         统计: totalCases, completedCases, passedCases
  │         计算: overallScore, trajectoryScore, llmJudgeScore 平均值
  │         状态: COMPLETED
  │
  └── 返回 EvaluationReport
```

---

## 三、8 大轨迹指标详解

| # | 指标 | 字段 | 计算方法 | 面试对应概念 |
|---|------|------|---------|------------|
| 1 | **步骤效率** | `stepEfficiency` | `min(1.0, 6 / 实际步数)` | Step Efficiency |
| 2 | **工具选择准确率** | `toolAccuracy` | 已知工具类型匹配的步骤占比 | Tool Selection Accuracy |
| 3 | **参数正确率** | `paramCorrectness` | SUCCESS 状态工具步骤占比 | Parameter Correctness |
| 4 | **冗余调用率** | `redundancyRate` | 重复 (phase:stepType) 调用 / 总工具调用 | Redundant Call Rate |
| 5 | **端到端延迟** | `totalDurationMs` | 所有步骤 durationMs 累加 | E2E Latency |
| 6 | **Token 消耗** | `totalTokens` | 所有步骤 tokensUsed 累加 | Token Consumption |
| 7 | **执行成功率** | `successRate` | SUCCESS 步骤 / 总步骤数 | Task Success Rate |
| 8 | **模型调用次数** | `totalModelCalls` | 所有步骤 modelCalls 累加 | Model Call Count |
| — | **总步骤数** | `totalSteps` | trace 列表长度 | — |
| — | **成功步骤数** | `successSteps` | status="SUCCESS" 的步骤数 | — |
| — | **有效工具调用** | `effectiveToolCalls` | 去重后的 phase:stepType 组合数 | — |
| — | **总工具调用** | `totalToolCalls` | toolCalls 累加 | — |

**代码位置**：`TrajectoryAnalyzer.java:55-99`

### 指标计算的关键逻辑

**工具准确率**：预定义了论文返修工作流的已知工具类型集合（`read_pdf`, `search_references`, `check_citation` 等），将步骤的 `stepType` 与该集合匹配。

**冗余检测**：使用 `Set<String>` 按 `phase:stepType` 去重，检测同一工具在同一阶段的重复调用。

**参数正确率**：当前基于步骤状态推断（`SUCCESS` → 参数基本正确），后续可接入独立的参数校验器。

---

## 四、LLM 裁判评分标准

### 评分维度和描述

```java
EvaluationRubric.REVISION_RUBRIC:

relevance (相关性):
  "评价修改内容是否针对审稿意见的核心问题。
   1=完全不相关，偏离核心问题
   5=完全命中核心问题，精准回应"

faithfulness (忠实度):
  "评价修改是否忠实于参考文献内容，没有捏造数据或结论。
   1=大量捏造，无法在参考文献中找到依据
   5=完全基于原文，引用准确"

completeness (完整性):
  "评价修改是否全面覆盖了审稿意见的所有子要求。
   1=完全遗漏多个关键要求
   5=全面覆盖所有子要求，无遗漏"

format (格式):
  "评价格式是否符合学术规范（引用格式、术语统一、结构清晰）。
   1=格式混乱，不符合基本规范
   5=完全符合学术写作规范"

hallucination (幻觉检测):
  "检查修改内容中是否存在：
   (a) 参考文献中不存在的数据或结论
   (b) 编造的引用来源
   (c) 与参考文本矛盾的陈述
   如发现幻觉，faithfulness 分应显著降低"
```

### LLM 裁判 Prompt 结构

```
You are an expert evaluator for academic paper revision quality.
Evaluate the following revision response on a 1-5 scale for each dimension.

## Scoring Dimensions (1-5 scale):
- relevance: ...
- faithfulness: ...
- completeness: ...
- format: ...

## Hallucination Detection:
- hallucination_detection: ...

## Original Text:
<原文，最多 2000 字符>

## Revised Text / Suggestion:
<修改后，最多 1500 字符>

## Revision Requirement:
<审稿意见>

## Reference Context:
<参考文献，最多 1500 字符>

## Output Format
Return ONLY a JSON object (no markdown, no code fences):
{
  "relevance": <int 1-5>,
  "faithfulness": <int 1-5>,
  "completeness": <int 1-5>,
  "format": <int 1-5>,
  "hallucination": <int 1-5>,
  "reasoning": "<step-by-step reasoning>"
}
```

### 分数标准化

```
LLM 返回 1-5 分制
    ↓ normalizeScore()
0.0 - 1.0 标准分制
    ↓ 加权融合 (LLM 65% + 轨迹 35%)
最终 4 维度评分 + overall
```

**代码位置**：`LLMJudgeServiceImpl.java:99-116 (prompt)` / `:164 (normalizeScore)`

---

## 五、数据表关系

```
agent_execution_traces (13 列)
├── id, session_id, agent_id, user_id
├── phase, step_type, input_data, output_data
├── model_calls, tool_calls, tokens_used, duration_ms
├── status
└── created_at, updated_at, deleted_at

evaluations (12 列)
├── id, revision_result_id, user_id
├── relevance_score, faithfulness_score
├── completeness_score, format_score
├── overall_score, feedback, evaluator_type
└── created_at, updated_at, deleted_at

agent_test_cases (12 列)
├── id, name, description
├── input_data, expected_output, ground_truth
├── source_dataset, difficulty
├── user_id
└── created_at, updated_at, deleted_at

agent_test_suites (8 列)
├── id, name, description
├── config_json, status
├── user_id
└── created_at, updated_at, deleted_at

agent_test_suite_cases (6 列)
├── id, suite_id, case_id, sort_order
└── created_at, updated_at, deleted_at
        ↑ N:N 关联

agent_eval_reports (15 列)
├── id, suite_id, agent_id, name
├── status, overall_score, trajectory_score, llm_judge_score
├── total_cases, completed_cases, passed_cases
├── summary, config_json, user_id
└── created_at, updated_at, deleted_at
        ↑ 聚合根

agent_eval_report_items (14 列)
├── id, report_id, case_id
├── overall_score, trajectory_score, llm_score
├── passed, feedback, trace_id
├── duration_ms, tokens_used, details_json
└── created_at, updated_at, deleted_at
        ↑ 聚合组成部分
```

---

## 六、DDD 设计决策

| 决策 | 理由 | 面试体现 |
|------|------|---------|
| **LLMJudgeService 接口在 Domain，实现在 Infra** | 依赖倒置原则，Domain 不应依赖具体的 LLM 调用方式 | "我们用 DIP 解耦了评估逻辑和具体的模型调用" |
| **EvaluationReport 作为聚合根** | 报告拥有其条目，通过 addItem/computeAggregates 维护一致性 | "EvaluationReport 是聚合根，所有条目修改必须通过报告" |
| **TrajectoryAnalyzer 是纯 Domain Service** | 无任何外部依赖，只做纯计算，方便单元测试 | "轨迹分析器是纯函数式设计，不依赖数据库或 LLM" |
| **AgentExecutionTrace 放在 evaluation 包** | 消费方是评估模块，遵循"谁消费谁定义" | "我们把 trace entity 放在 evaluation 包，因为评估才是 trace 的消费者" |
| **四个值对象 (Value Object)** | TrajectoryEvalResult / LLMJudgeRequest / LLMJudgeResult / EvaluationRubric 都是不可变的，只通过构造函数赋值 | "值对象保证不可变性，避免副作用" |
| **降级策略内置在领域服务** | LLM 调用失败 → 自动回退到纯轨迹评估，evaluatorType 标记为 "TRAJECTORY" | "评估系统有完整的降级策略，LLM 不可用不影响核心流程" |

---

## 七、核心类清单

### Domain 层 (17 个文件)

| 文件 | 类型 | 职责 |
|------|------|------|
| `domain/evaluation/model/EvaluationEntity.java` | Entity | 映射 `evaluations` 表 |
| `domain/evaluation/model/AgentExecutionTraceEntity.java` | Entity | 映射 `agent_execution_traces` 表 |
| `domain/evaluation/model/TestCaseEntity.java` | Entity | 映射 `agent_test_cases` 表 |
| `domain/evaluation/model/TestSuiteEntity.java` | Entity | 映射 `agent_test_suites` 表 |
| `domain/evaluation/model/TestSuiteCaseEntity.java` | Entity | 映射 `agent_test_suite_cases` 表 |
| `domain/evaluation/model/EvaluationReport.java` | Aggregate Root | 评估报告，管理 EvalReportItem |
| `domain/evaluation/model/EvalReportItem.java` | Entity | 单条用例评估结果 |
| `domain/evaluation/model/TrajectoryEvalResult.java` | Value Object | 8 项轨迹指标（不可变） |
| `domain/evaluation/model/LLMJudgeRequest.java` | Value Object | LLM 裁判请求（不可变） |
| `domain/evaluation/model/LLMJudgeResult.java` | Value Object | LLM 裁判 5 维结果（不可变） |
| `domain/evaluation/model/EvaluationRubric.java` | Value Object | 预定义评分标准 |
| `domain/evaluation/service/EvaluationDomainService.java` | Domain Service | 评估编排：轨迹 + LLM → 加权融合 |
| `domain/evaluation/service/TrajectoryAnalyzer.java` | Domain Service | 纯计算：trace → 8 指标 |
| `domain/evaluation/service/LLMJudgeService.java` | Domain Interface | LLM 裁判接口 (DIP) |
| `domain/evaluation/service/TestSuiteExecutor.java` | Domain Service | 批量执行测试套件 |
| `domain/evaluation/repository/` (6 个) | Repository | MyBatis-Plus Mapper 接口 |
| `domain/trace/service/ExecutionTraceDomainService.java` | Domain Service | Trace 写入 + 查询（DB 持久化） |

### Application 层 (6 个文件)

| 文件 | 职责 |
|------|------|
| `dto/EvaluationDTO.java` | 评估结果传输对象 |
| `dto/TrajectoryEvalResultDTO.java` | 轨迹指标传输对象 |
| `assembler/EvaluationAssembler.java` | Entity ↔ DTO 转换 |
| `service/EvaluationAppService.java` | 评估查询编排 |

### Infrastructure 层 (1 个文件)

| 文件 | 职责 |
|------|------|
| `infrastructure/evaluation/LLMJudgeServiceImpl.java` | 调用 LLMService → 构造 prompt → 解析 JSON 响应 |

### Interface 层 (3 个文件)

| 文件 | 端点 |
|------|------|
| `interfaces/api/portal/evaluation/EvaluationController.java` | `GET /api/evaluation/trajectory\|history` |
| `interfaces/api/portal/evaluation/TestCaseController.java` | `CRUD /api/evaluation/test-cases` |
| `interfaces/api/portal/evaluation/TestSuiteController.java` | `CRUD /api/evaluation/test-suites` + run/reports |

---

## 八、API 端点参考

| 方法 | 路径 | 说明 | Phase |
|------|------|------|-------|
| `GET` | `/api/evaluation/history/{resultId}` | 查询评估历史 | P1 |
| `GET` | `/api/evaluation/trajectory/{sessionId}` | 独立轨迹分析（8 指标） | P1 |
| `GET` | `/api/evaluation/test-cases` | 列出测试用例 | P3 |
| `POST` | `/api/evaluation/test-cases` | 创建测试用例 | P3 |
| `GET` | `/api/evaluation/test-cases/{id}` | 用例详情 | P3 |
| `PUT` | `/api/evaluation/test-cases/{id}` | 更新用例 | P3 |
| `DELETE` | `/api/evaluation/test-cases/{id}` | 删除用例 | P3 |
| `GET` | `/api/evaluation/test-suites` | 列出测试套件 | P3 |
| `POST` | `/api/evaluation/test-suites` | 创建套件 | P3 |
| `GET` | `/api/evaluation/test-suites/{id}` | 套件详情 | P3 |
| `PUT` | `/api/evaluation/test-suites/{id}` | 更新套件 | P3 |
| `DELETE` | `/api/evaluation/test-suites/{id}` | 删除套件 | P3 |
| `POST` | `/api/evaluation/test-suites/{id}/cases` | 向套件添加用例 | P3 |
| `DELETE` | `/api/evaluation/test-suites/{id}/cases/{caseId}` | 从套件移除用例 | P3 |
| `POST` | `/api/evaluation/test-suites/{id}/run` | 执行测试套件 | P3 |
| `GET` | `/api/evaluation/test-suites/{id}/reports` | 套件报告列表 | P3 |
| `GET` | `/api/evaluation/test-suites/reports/{id}/items` | 报告明细 | P3 |

**主入口**（在 RevisionController 中）：

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/revision/execute` | 执行返修后自动触发评估（P1+P2 全部通路） |

---

## 九、面试话术

### 开场：架构总览

> "我们的评估系统不是简单的硬编码打分，而是**三层评估体系**。
>
> 第一层是**轨迹评估**——从 Agent 执行追踪中提取 8 项指标，包括步骤效率、
> 工具准确率、冗余调用率、端到端延迟等，对应业界标准的 Agent 评估框架。
>
> 第二层是 **LLM 裁判**——用用户配置的 LLM（DeepSeek/Doubao）做结构化语义评估，
> 5 个维度加幻觉检测。LLM 裁判服务接口定义在 Domain 层，实现在 Infrastructure 层，
> 这是依赖倒置原则的体现。
>
> 第三层是**测试套件**——支持自定义用例、从 GAIA/HotPotQA 导入，
> 批量运行后生成聚合报告。"

### 指标设计

> "轨迹分析器是纯领域服务，不依赖任何外部基础设施。它从 `agent_execution_traces`
> 表中读取步骤级数据，计算包括步骤效率（实际/预期步数比）、工具选择准确率
> （基于预定义的工具类型集合做匹配）、冗余调用率（按 phase:stepType 去重）、
> 端到端延迟和 Token 消耗等 8 个维度的指标。"

### LLM Judge 设计

> "LLM 裁判的核心是结构化 prompt。我们预制了 `EvaluationRubric` 评分标准，
> 每个维度有 1-5 分的详细描述。LLM 返回 JSON，我们解析后把 1-5 分标准化为
> 0.0-1.0，然后和轨迹分析结果加权融合。LLM 占 65%，轨迹占 35%。
> 关键的工程设计是**降级策略**——LLM 调用失败时自动回退到纯轨迹评估，
> 不会让整个评估链路崩溃。"

### 测试套件设计

> "测试套件模块支持'测试用例集 + 执行引擎 + 结果统计'的标准模式。
> 每个用例有输入、期望输出、来源数据集（GAIA/HotPotQA/自定义）等字段。
> 套件执行时生成 `EvaluationReport`，它是一个 DDD 聚合根，
> 所有条目通过 `addItem()` 添加，最后 `computeAggregates()` 计算
> 通过率、平均分等统计指标。"

### 降级策略

> "评估系统的可靠性设计是：`evaluatorType` 字段区分 `LLM_JUDGE` 和 `TRAJECTORY`。
> LLM Judge 调用失败时，系统自动回退到纯轨迹评估，不影响用户的核心流程。
> 这样即使 LLM 服务不稳定，评估模块也不会成为单点故障。"

---

## 附录：评估理论 ↔ 代码映射速查表

| 评估理论概念 | 代码位置 |
|-------------|---------|
| 任务成功率 (Task Success Rate) | `TrajectoryAnalyzer.getSuccessRate()` |
| 步骤效率 (Step Efficiency) | `TrajectoryAnalyzer.getStepEfficiency()` |
| 工具选择准确率 | `TrajectoryAnalyzer.getToolAccuracy()` |
| 冗余调用率 | `TrajectoryAnalyzer.getRedundancyRate()` |
| 端到端延迟 (E2E Latency) | `TrajectoryAnalyzer.getTotalDurationMs()` |
| Token 消耗 | `TrajectoryAnalyzer.getTotalTokens()` |
| LLM 裁判 (Judge LLM) | `LLMJudgeServiceImpl.evaluate()` |
| 幻觉检测 | `LLMJudgeResult.getHallucinationScore()` |
| 评分标准 (Rubric) | `EvaluationRubric.REVISION_RUBRIC` |
| 降级策略 | `EvaluationDomainService` try-catch 逻辑 |
| 测试用例集 | `TestCaseEntity` + `TestSuiteEntity` |
| 聚合报告 | `EvaluationReport.computeAggregates()` |
| pass@k | `TestSuiteExecutor` 可扩展 k 次重试 |
| GAIA / HotPotQA / SWE-bench | `TestCaseEntity.sourceDataset` 字段 |
| DDD 依赖倒置 | `LLMJudgeService` (Domain 接口) → `LLMJudgeServiceImpl` (Infra 实现) |
| DDD 聚合根 | `EvaluationReport` 聚合 `EvalReportItem` |
| DDD 值对象 | `TrajectoryEvalResult`, `LLMJudgeResult`, `EvaluationRubric` |
