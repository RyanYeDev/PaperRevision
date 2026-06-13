"use client";

import Link from "next/link";

export default function Home() {
  return (
    <div className="space-y-12">
      <section className="text-center py-16">
        <h2 className="text-4xl font-bold text-gray-900 mb-4">
          智能论文返修平台
        </h2>
        <p className="text-lg text-gray-600 mb-8 max-w-2xl mx-auto">
          基于AI Agent技术，支持上传论文PDF、参考文献和返修意见，
          通过RAG检索+工作流引擎自动生成修改建议或直接修改论文。
        </p>
        <div className="flex gap-4 justify-center">
          <Link href="/papers" className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
            上传论文
          </Link>
          <Link href="/revision" className="px-6 py-3 border border-gray-300 rounded-lg hover:bg-gray-50">
            开始返修
          </Link>
        </div>
      </section>

      <section className="grid grid-cols-3 gap-6">
        {[
          { title: "PDF解析", desc: "支持论文PDF上传与自动文本解析" },
          { title: "RAG检索", desc: "基于Milvus的参考文献向量检索" },
          { title: "Workflow引擎", desc: "DAG工作流驱动的返修流程" },
          { title: "Tool Calling", desc: "内置8个学术写作工具" },
          { title: "MCP集成", desc: "Model Context Protocol工具扩展" },
          { title: "全链路追踪", desc: "Agent执行过程可观测" },
        ].map((f) => (
          <div key={f.title} className="p-6 bg-white rounded-lg shadow-sm border">
            <h3 className="font-semibold text-gray-900 mb-2">{f.title}</h3>
            <p className="text-sm text-gray-600">{f.desc}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
