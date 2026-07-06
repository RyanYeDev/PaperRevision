"use client";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import "./globals.css";

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <head>
        <title>PaperRevision - 智能论文返修平台</title>
        <meta name="description" content="上传论文PDF，输入返修意见，AI Agent自动分析并给出修改方案" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='0.9em' font-size='90'>📝</text></svg>" />
      </head>
      <body>
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
    <nav className="nav-cartoon sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        <a href="/" className="text-2xl font-bold" style={{ color: 'var(--primary)' }}>
          &#x1F4DD; PaperRevision
        </a>
        <div className="flex items-center gap-5 text-sm font-medium">
          {user ? (
            <>
              <a href="/papers" style={{ color: 'var(--text)' }} className="hover:opacity-70">&#x1F4C4; 论文</a>
              <a href="/revision" style={{ color: 'var(--text)' }} className="hover:opacity-70">&#x2728; 返修</a>
              <a href="/evaluation" style={{ color: 'var(--text)' }} className="hover:opacity-70">&#x1F4CA; 评估</a>
              <a href="/settings" style={{ color: 'var(--text)' }} className="hover:opacity-70">&#x2699; 设置</a>
              <span style={{ color: 'var(--text-light)' }}>{user.nickname}</span>
              <button onClick={logout} style={{ color: 'var(--danger)' }} className="hover:opacity-70">退出</button>
            </>
          ) : (
            <>
              <a href="/auth/login" style={{ color: 'var(--text)' }}>登录</a>
              <a href="/auth/register" className="px-4 py-1.5 text-white rounded-full text-xs font-bold btn-cartoon" style={{ background: 'var(--primary)' }}>注册</a>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
