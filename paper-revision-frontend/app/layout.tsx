"use client";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import { usePathname } from "next/navigation";
import "./globals.css";

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <head>
        <title>PaperRevision - 智能论文返修平台</title>
        <meta name="description" content="上传论文PDF，输入返修意见，AI Agent自动分析并给出修改方案" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='0.9em' font-size='90'>📝</text></svg>" />
        {/* 圆润拉丁字体：Baloo 2(显示) + Nunito(正文)，与顶会体(中文)配对 */}
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Baloo+2:wght@500;600;700;800&family=Nunito:wght@400;500;600;700;800&display=swap"
          rel="stylesheet"
        />
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
  const pathname = usePathname();
  const links = [
    { href: "/papers", label: "论文", icon: "📄" },
    { href: "/revision", label: "返修", icon: "✨" },
    { href: "/evaluation", label: "评估", icon: "📊" },
    { href: "/settings", label: "设置", icon: "⚙️" },
  ];

  return (
    <nav className="nav-cartoon sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        <a href="/" className="text-2xl font-display font-extrabold tracking-tight"
          style={{ background: "linear-gradient(135deg, var(--primary), var(--primary-deep))", WebkitBackgroundClip: "text", backgroundClip: "text", color: "transparent" }}>
          📝 PaperRevision
        </a>
        <div className="flex items-center gap-5 text-sm font-bold">
          {user ? (
            <>
              {links.map(l => (
                <a key={l.href} href={l.href}
                  className={`nav-link hover:opacity-80 ${pathname === l.href ? "active" : ""}`}
                  style={{ color: pathname === l.href ? "var(--primary-deep)" : "var(--text)" }}>
                  {l.icon} {l.label}
                </a>
              ))}
              <span style={{ color: "var(--text-light)" }}>{user.nickname}</span>
              <button onClick={logout} className="hover:opacity-70" style={{ color: "var(--danger)" }}>退出</button>
            </>
          ) : (
            <>
              <a href="/auth/login" className="nav-link" style={{ color: "var(--text)" }}>登录</a>
              <a href="/auth/register" className="btn-cartoon btn-primary px-4 py-1.5 rounded-full text-xs">注册</a>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
