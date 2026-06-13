import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "PaperRevision - 智能论文返修平台",
  description: "基于AI Agent的智能论文返修平台，支持RAG检索、工作流引擎和全链路追踪",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen bg-gray-50">
        <nav className="bg-white shadow-sm border-b">
          <div className="max-w-7xl mx-auto px-4 h-14 flex items-center justify-between">
            <h1 className="text-xl font-bold text-gray-900">PaperRevision</h1>
            <div className="flex gap-4 text-sm">
              <a href="/" className="text-gray-600 hover:text-gray-900">首页</a>
              <a href="/papers" className="text-gray-600 hover:text-gray-900">论文管理</a>
              <a href="/revision" className="text-gray-600 hover:text-gray-900">返修中心</a>
              <a href="/settings" className="text-gray-600 hover:text-gray-900">设置</a>
            </div>
          </div>
        </nav>
        <main className="max-w-7xl mx-auto px-4 py-6">{children}</main>
      </body>
    </html>
  );
}
