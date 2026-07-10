"use client";
import { cn } from "@/lib/utils";
import { TextareaHTMLAttributes } from "react";

interface Props extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
}

// 使用统一的 .field 焦点态，替换旧的 focus:ring-blue-500。
export function Textarea({ label, className, ...props }: Props) {
  return (
    <div className="space-y-1">
      {label && <label className="block text-sm font-bold text-[color:var(--text)]">{label}</label>}
      <textarea className={cn("field resize-y", className)} {...props} />
    </div>
  );
}
