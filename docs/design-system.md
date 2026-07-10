# PaperRevision 前端设计系统

> 本文档是一次 **Skill 自进化实践** 的产物：我们把外部 `frontend-slides` skill 的设计哲学"学习并内化"为项目自己的设计系统，让前端能力从"随手拼样式"进化为"统一设计语言驱动"。
>
> 审美方向：**清新升级 / Refined Fresh**（清新卡通蓝黄，硬化 commit）。

---

## 一、从 frontend-slides 学到的核心原则

`frontend-slides` skill 本是做 HTML 演示文稿的，但它的设计方法论是通用且可迁移的。我们提炼并采纳了 5 条：

1. **拒绝"AI slop"**
   不用 Inter/Roboto/系统字体做显示字体，不用紫渐变白底，不用千篇一律的居中英雄区 + 相同卡片网格。每个界面要像"为这个场景专门设计的"。

2. **单一事实来源的设计 token**
   所有颜色、圆角、阴影、字体、缓动都用 CSS 变量集中定义（见 `globals.css :root`），改一处即改全站。杜绝组件各写各的 `bg-blue-600`。

3. **committed 配色 > 怯生生的均匀调色板**
   一个饱和的主色（加深后的蓝 `#5b9fd4`）+ 一个尖锐点缀（暖黄 `#f2c94c`），比一堆浅淡色更有记忆点。

4. **氛围与深度的背景**
   不要纯色平铺。用分层径向渐变 mesh（多个柔和光斑叠加在暖渐变底上）营造空间感。

5. **一次编排好的 staggered 载入动画 > 零散微交互**
   页面载入时用 `animation-delay` 阶梯让元素错峰入场（`.reveal` + `.reveal-1..6`），比到处塞 hover 抖动更有质感。

---

## 二、设计 token 速查

| 类别 | 变量 | 值 | 用途 |
| --- | --- | --- | --- |
| 主色 | `--primary` | `#5b9fd4` | 主蓝（加深强化） |
| | `--primary-deep` | `#3d7fb8` | 渐变/悬停/强调 |
| | `--primary-soft` | `#8bb8d0` | 旧主色，作淡色调 |
| | `--primary-tint` | `#eaf3f9` | 选中/高亮块底 |
| 点缀 | `--accent` | `#f2c94c` | 暖黄 |
| | `--accent-deep` | `#e0af2e` | 黄的深阶 |
| | `--accent-ink` | `#7a6200` | 黄底上的可读文字 |
| 语义 | `--success` / `--danger` | `#7bc89c` / `#e88a8a` | 成功/危险 |
| 文字 | `--text` / `--text-light` | `#3f4a5a` / `#94a3b8` | 主/次文字 |
| 圆角 | `--radius` / `-sm` / `-lg` | `16` / `10` / `22` px | 卡片/控件/大卡 |
| 字体 | `--font-display` | Baloo 2 → 顶会体 | 标题 |
| | `--font-body` | Nunito → 顶会体 | 正文 |
| 缓动 | `--ease-out-expo` | `cubic-bezier(.16,1,.3,1)` | 通用出场 |
| | `--ease-spring` | `cubic-bezier(.34,1.56,.64,1)` | 按钮弹性 |

字体配对逻辑：中文由**顶会体**兜底渲染，拉丁字母/数字由圆润的 **Baloo 2 / Nunito** 渲染，两者气质一致（都圆润友好）。

---

## 三、可复用类（在 `globals.css` 定义）

| 类 | 作用 |
| --- | --- |
| `.paper-card` | 圆角卡片 + 柔和阴影 + 悬停上浮 |
| `.feature-card` | 悬停时透出主色→暖黄的顶边 |
| `.btn-cartoon` | 弹性按压动画（配合 `.btn-primary`/`.btn-accent`） |
| `.btn-primary` / `.btn-accent` | 主色渐变按钮 / 暖黄渐变按钮 |
| `.nav-cartoon` / `.nav-link` | 毛玻璃导航 / 悬停下划线（`.active` 常亮） |
| `.field` | 统一表单控件（主色描边焦点 + 柔和光晕） |
| `.badge` + `.badge-primary/success/warn` | 圆角徽章 |
| `.reveal` + `.reveal-1..6` | staggered 载入动画（阶梯延迟） |
| `.float-soft` | 装饰光斑的呼吸浮动 |

Tailwind 侧已在 `tailwind.config.ts` 接入同名 token，可写 `bg-primary`、`text-accent`、`rounded-card`、`font-display` 等语义类。

---

## 四、使用约定

- **新页面/组件**：优先用上面的可复用类和 CSS 变量，不要引入新的裸色值（如 `bg-blue-600`、`text-gray-500`）。
- **入场动画**：容器内同级子项加 `reveal reveal-{序号}`，序号从 1 递增即可错峰。
- **配色克制**：一屏内主色出现应聚焦（按钮、强调数字、活跃态），暖黄只作点睛，不要大面积铺。
- **无障碍**：`prefers-reduced-motion` 已全局兜底，新增动画无需重复处理。

---

## 五、本次改造覆盖范围（2026-07-10）

- 核心：`app/globals.css`（设计系统重写）、`tailwind.config.ts`（token 接入）
- 组件：`Button` / `Card` / `Input` / `Textarea` 统一到 token
- 页面：`layout`（导航+字体）、首页 Hero、`papers`、`login`/`register`、`settings` 去 `bg-blue-600` 漂移

后续 `revision` / `evaluation` 页已大量使用 token，会随 `globals.css` 自动升级；残留的少量 `bg-gray-*` 信息块留待每日改进任务逐步打磨。
