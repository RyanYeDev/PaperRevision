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

export default function Home() {
  return (
    <div className="space-y-12 pb-12">
      {/* Hero */}
      <section className="text-center py-20">
        <h2 className="text-5xl font-bold mb-4" style={{ color: 'var(--primary)' }}>
          &#x2728; 智能论文返修平台
        </h2>
        <p className="text-lg mb-8 max-w-xl mx-auto" style={{ color: 'var(--text-light)' }}>
          上传论文PDF → 输入返修意见 → AI Agent自动分析并给出修改方案
        </p>
        <div className="flex gap-4 justify-center">
          <Link href="/papers" className="px-8 py-3.5 text-white rounded-full text-lg font-bold btn-cartoon"
            style={{ background: 'var(--primary)' }}>
            &#x1F4C4; 上传论文
          </Link>
          <Link href="/revision" className="px-8 py-3.5 rounded-full text-lg font-bold btn-cartoon paper-card"
            style={{ color: 'var(--text)' }}>
            &#x2728; 开始返修
          </Link>
        </div>
      </section>

      {/* Features */}
      <section className="grid grid-cols-3 gap-5">
        {features.map((f, i) => (
          <div key={f.title} className="paper-card p-6">
            <div className="text-3xl mb-3">{emojis[i]}</div>
            <h3 className="font-bold text-lg mb-2">{f.title}</h3>
            <p style={{ color: 'var(--text-light)' }} className="text-sm leading-relaxed">{f.desc}</p>
          </div>
        ))}
      </section>

      {/* Stats */}
      <section className="text-center py-8">
        <div className="inline-flex gap-8 px-8 py-4 paper-card rounded-full">
          {[
            { n: "4层", l: "PDF解析降级" },
            { n: "8个", l: "内置工具" },
            { n: "7步", l: "返修工作流" },
            { n: "100%", l: "API可用" },
          ].map(s => (
            <div key={s.l} className="text-center">
              <div className="text-2xl font-bold" style={{ color: 'var(--primary)' }}>{s.n}</div>
              <div className="text-xs" style={{ color: 'var(--text-light)' }}>{s.l}</div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
