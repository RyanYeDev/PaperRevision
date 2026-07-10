"use client";
import { cn } from "@/lib/utils";

// 使用全站统一的 .paper-card（圆角 + 柔和阴影 + 悬停上浮），不再用通用 bg-white/rounded-lg。
export function Card({ className, children }: { className?: string; children: React.ReactNode }) {
  return <div className={cn("paper-card", className)}>{children}</div>;
}
export function CardHeader({ className, children }: { className?: string; children: React.ReactNode }) {
  return <div className={cn("px-6 py-4 border-b border-[color:var(--border)]", className)}>{children}</div>;
}
export function CardContent({ className, children }: { className?: string; children: React.ReactNode }) {
  return <div className={cn("px-6 py-4", className)}>{children}</div>;
}
