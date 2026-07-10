"use client";
import { cn } from "@/lib/utils";
import { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "secondary" | "accent" | "danger" | "ghost";
type Size = "sm" | "md" | "lg";

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
}

// 全部走设计 token（.btn-primary / .btn-accent 等在 globals.css 定义），
// 不再使用 bg-blue-600 之类的通用色，保证与全站一致。
const styles: Record<Variant, string> = {
  primary: "btn-primary",
  accent: "btn-accent",
  secondary: "bg-white text-[color:var(--text)] border-[color:var(--border)] hover:bg-[color:var(--primary-tint)]",
  danger: "text-white border-transparent",
  ghost: "bg-transparent text-[color:var(--text-light)] border-transparent hover:bg-[color:var(--primary-tint)]",
};

const sizes: Record<Size, string> = {
  sm: "px-3 py-1.5 text-xs",
  md: "px-4 py-2 text-sm",
  lg: "px-6 py-3 text-base",
};

export function Button({ variant = "primary", size = "md", loading, className, children, disabled, style, ...props }: Props) {
  return (
    <button
      className={cn(
        "btn-cartoon inline-flex items-center justify-center gap-2 rounded-soft border font-bold disabled:opacity-50 disabled:cursor-not-allowed",
        styles[variant], sizes[size], className
      )}
      disabled={disabled || loading}
      style={variant === "danger" ? { background: "var(--danger)", ...style } : style}
      {...props}
    >
      {loading && <span className="animate-spin text-xs">&#9696;</span>}
      {children}
    </button>
  );
}
