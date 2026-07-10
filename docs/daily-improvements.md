# 每日改进记录

> 自动改进任务：每天 12:32 执行，改动 ≤50 行，不引入新依赖，不修改核心业务逻辑

---

## 2026-07-10

- **所属路线**：Skill 自动进化（实践扩展）—— 学习外部 `frontend-slides` skill 的设计哲学并内化为项目设计系统
- **前端设计系统大改（清新升级 / Refined Fresh）**：
  - 建立单一事实来源的设计 token（`globals.css :root`）：加深主蓝 `#5b9fd4`、暖黄点缀、分层渐变 mesh 背景、字体配对（Baloo 2 + Nunito + 顶会体）、staggered 载入动画系统（`.reveal`）
  - `tailwind.config.ts` 接入 token，可用 `bg-primary`/`font-display` 等语义类
  - 统一 `Button`/`Card`/`Input`/`Textarea` 到 token，消除 `bg-blue-600` 设计漂移
  - 改造 `layout`（毛玻璃导航活跃态 + 字体 link）、首页 Hero（氛围光斑 + 阶梯入场）、`papers`/`login`/`register`/`settings` 页
  - 新增 `docs/design-system.md` 沉淀设计原则（Skill 自进化产物）
- 影响范围：`app/globals.css`(重写)、`tailwind.config.ts`、`components/ui/*`(4 个)、`app/layout.tsx`、`app/page.tsx`、`app/papers/page.tsx`、`app/auth/*`、`app/settings/page.tsx`、`docs/design-system.md`(新)
- 备注：本次为用户驱动的大改，超出每日 ≤50 行约束

---

## 2026-07-08

- **API 错误消息透传**：`HttpClient.request()` 捕获非 200 响应时，解析后端返回的 JSON `message` 字段，替换原有的通用 `API Error: 状态码` 提示，用户可见具体错误（如"邮箱已被注册"）
- **消除 API_BASE 硬编码**：导出 `API_BASE` 常量供全项目复用，`AuthContext` 和 `settings/page.tsx` 中原有的 6 处 `http://localhost:8088/api` 硬编码改为引用统一常量
- 影响范围：`lib/api.ts`(修 4 行)、`contexts/AuthContext.tsx`(修 4 行)、`app/settings/page.tsx`(修 6 行)（总计 +14 行）

---

## 2026-07-07

- **SkillRegistry 单元测试**：为昨日新增的 `recordUsage()`、`getTopUsedSkills()`、`getTopPerformingSkills()`、`getStaleSkills()` 方法补充 7 个单元测试
- **修复日志格式化**：`SkillRegistry.recordUsage()` 中 SLF4J 占位符 `{:.0%}` 不支持格式化，改为 `String.format("%.0f%%", ...)` 方式
- 影响范围：`domain/tool/service/SkillRegistry.java`(修3行)、测试新增(80行)

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
