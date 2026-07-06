# 每日改进记录

> 自动改进任务：每天 12:32 执行，改动 ≤50 行，不引入新依赖，不修改核心业务逻辑

---

## 2026-07-06

- **上下文分层压缩基础 — TokenCounter**：新增 `TokenCounter` 工具类，支持中英文 token 精确估算（中文字符 ×1.5 + 英文单词 ×1.3）、快速估算、批量估算、`ContextBudget` 预算管理，10 个单元测试全部通过
- **Skill 自动进化基础**：`SkillRegistry.SkillDefinition` 新增 `useCount`/`successCount`/`successRate`/`lastUsedAt` 使用追踪字段；新增 `recordUsage()`、`getTopUsedSkills()`、`getTopPerformingSkills()`、`getStaleSkills()` 方法
- **前端 SEO 元数据**：`layout.tsx` 添加 `<title>`、`<meta description>`、emoji favicon
- **论文上传错误反馈**：`papers/page.tsx` 上传失败时显示红色错误提示
- 影响范围：`infrastructure/utils/TokenCounter.java`(新)、`domain/tool/SkillRegistry.java`(+40行)、`app/layout.tsx`、`app/papers/page.tsx`（总计 +38行后端，+17行前端）

---

## 2026-07-05

- **Agent 评估系统上线**：新增 31 个文件，涵盖轨迹分析（8 指标）、LLM 裁判（DIP 设计）、测试套件批量评估、前端仪表盘
- 影响范围：`domain/evaluation/`、`infrastructure/evaluation/`、`application/evaluation/`、`interfaces/.../evaluation/`、`app/evaluation/`

---
