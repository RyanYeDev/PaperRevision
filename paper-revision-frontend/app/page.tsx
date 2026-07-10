"use client";
import Link from "next/link";

const emojis = ["📄", "🔍", "⚙️", "🛠️", "🔌", "📊"];
const features = [
  { title: "PDF解析", desc: "四层降级策略：GROBID→PDFBox坐标→纯文本→修复重试，永不失败" },
  { title: "RAG检索", desc: "Milvus向量检索 + 关键词混合搜索，精准定位参考文献" },
  { title: "Workflow", desc: "DAG工作流引擎，7步论文返修流程，可自定义扩展" },
  { title: "Tool Calling", desc: "内置8个学术工具 + MCP协议，Agent可调用任意外部能力" },
  { title: "MCP集成", desc: "Model Context Protocol原生支持，工具生态无限扩展" },
  { title: "全链路追踪", desc: "Agent执行过程可观测，每步trace可查，返修结果可评估" },
];

const stats = [
  { n: "4层", l: "PDF解析降级" },
  { n: "8个", l: "内置工具" },
  { n: "7步", l: "返修工作流" },
  { n: "100%", l: "API可用" },
];

export default function Home() {
  return (
    <div className="space-y-14 pb-12">
      {/* ===== Hero ===== */}
      <section className="relative text-center py-20">
        {/* 氛围光斑：轻微呼吸的柔和渐变球 */}
        <div className="float-soft pointer-events-none absolute -z-10 left-1/4 top-8 h-40 w-40 rounded-full blur-3xl"
          style={{ background: "radial-gradient(circle, rgba(91,159,212,0.35), transparent 70%)" }} />
        <div className="float-soft pointer-events-none absolute -z-10 right-1/4 top-24 h-32 w-32 rounded-full blur-3xl"
          style={{ background: "radial-gradient(circle, rgba(242,201,76,0.30), transparent 70%)", animationDelay: "2s" }} />

        <div className="reveal reveal-1 inline-flex items-center gap-2 px-4 py-1.5 rounded-full text-xs font-bold badge-primary mb-6">
          ✨ AI Agent 驱动 · DDD 架构
        </div>
        <h1 className="reveal reveal-2 text-5xl md:text-6xl font-display font-extrabold mb-5"
          style={{ background: "linear-gradient(135deg, var(--primary), var(--primary-deep))", WebkitBackgroundClip: "text", backgroundClip: "text", color: "transparent" }}>
          智能论文返修平台
        </h1>
        <p className="reveal reveal-3 text-lg mb-9 max-w-xl mx-auto" style={{ color: "var(--text-light)" }}>
          上传论文 PDF → 输入返修意见 → AI Agent 自动分析并给出修改方案
        </p>
        <div className="reveal reveal-4 flex gap-4 justify-center">
          <Link href="/papers" className="btn-cartoon btn-primary px-8 py-3.5 rounded-full text-lg">
            📄 上传论文
          </Link>
          <Link href="/revision" className="btn-cartoon paper-card px-8 py-3.5 rounded-full text-lg font-bold"
            style={{ color: "var(--text)" }}>
            ✨ 开始返修
          </Link>
        </div>
      </section>

      {/* ===== Features ===== */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {features.map((f, i) => (
          <div key={f.title} className={`paper-card feature-card reveal reveal-${(i % 6) + 1} p-6`}>
            <div className="text-3xl mb-3">{emojis[i]}</div>
            <h3 className="font-display font-bold text-lg mb-2" style={{ color: "var(--text)" }}>{f.title}</h3>
            <p style={{ color: "var(--text-light)" }} className="text-sm leading-relaxed">{f.desc}</p>
          </div>
        ))}
      </section>

      {/* ===== Stats ===== */}
      <section className="text-center">
        <div className="reveal reveal-5 inline-flex flex-wrap justify-center gap-8 px-10 py-5 paper-card rounded-full">
          {stats.map(s => (
            <div key={s.l} className="text-center">
              <div className="text-2xl font-display font-extrabold"
                style={{ background: "linear-gradient(135deg, var(--primary), var(--primary-deep))", WebkitBackgroundClip: "text", backgroundClip: "text", color: "transparent" }}>
                {s.n}
              </div>
              <div className="text-xs" style={{ color: "var(--text-light)" }}>{s.l}</div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
