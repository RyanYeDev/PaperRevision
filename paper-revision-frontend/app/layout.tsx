"use client";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import "./globals.css";

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen bg-gray-50">
        <AuthProvider>
          <NavBar />
          <main className="max-w-7xl mx-auto px-4 py-6">{children}</main>
        </AuthProvider>
      </body>
    </html>
  );
}

function NavBar() {
  const { user, logout } = useAuth();
  return (
    <nav className="bg-white shadow-sm border-b">
      <div className="max-w-7xl mx-auto px-4 h-14 flex items-center justify-between">
        <a href="/" className="text-xl font-bold text-gray-900">PaperRevision</a>
        <div className="flex items-center gap-4 text-sm">
          {user ? (
            <>
              <a href="/papers" className="text-gray-600 hover:text-gray-900">论文管理</a>
              <a href="/revision" className="text-gray-600 hover:text-gray-900">返修中心</a>
              <a href="/settings" className="text-gray-600 hover:text-gray-900">设置</a>
              <span className="text-gray-400">|</span>
              <span className="text-gray-500">{user.nickname || user.email}</span>
              <button onClick={logout} className="text-gray-400 hover:text-red-500">退出</button>
            </>
          ) : (
            <>
              <a href="/auth/login" className="text-gray-600 hover:text-gray-900">登录</a>
              <a href="/auth/register" className="px-3 py-1 bg-blue-600 text-white rounded-lg text-xs hover:bg-blue-700">注册</a>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
