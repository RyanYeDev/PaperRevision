"use client";
import { cn } from "@/lib/utils";
import { InputHTMLAttributes } from "react";

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

// 使用统一的 .field 焦点态（主色描边 + 柔和光晕），替换旧的 focus:ring-blue-500。
export function Input({ label, error, className, ...props }: Props) {
  return (
    <div className="space-y-1">
      {label && <label className="block text-sm font-bold text-[color:var(--text)]">{label}</label>}
      <input className={cn("field", error && "border-[color:var(--danger)]", className)} {...props} />
      {error && <p className="text-xs text-[color:var(--danger)]">{error}</p>}
    </div>
  );
}
