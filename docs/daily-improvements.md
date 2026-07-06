# 每日改进记录

> 自动改进任务：每天 12:32 执行，改动 ≤50 行，不引入新依赖，不修改核心业务逻辑

---

## 2026-07-06

- **前端 SEO 元数据**：`layout.tsx` 添加 `<title>`、`<meta description>`、emoji favicon，浏览器标签不再只显示URL
- **论文上传错误反馈**：`papers/page.tsx` 上传失败时显示红色错误提示，可手动关闭
- 影响范围：`app/layout.tsx`、`app/papers/page.tsx`（+17行）

---

## 2026-07-05

- **Agent 评估系统上线**：新增 31 个文件，涵盖轨迹分析（8 指标）、LLM 裁判（DIP 设计）、测试套件批量评估、前端仪表盘
- 影响范围：`domain/evaluation/`、`infrastructure/evaluation/`、`application/evaluation/`、`interfaces/.../evaluation/`、`app/evaluation/`

---
