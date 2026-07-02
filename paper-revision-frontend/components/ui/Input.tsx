"use client";
import { cn } from "@/lib/utils";
import { InputHTMLAttributes } from "react";

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export function Input({ label, error, className, ...props }: Props) {
  return (
    <div className="space-y-1">
      {label && <label className="block text-sm font-medium text-gray-700">{label}</label>}
      <input className={cn("w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent", error && "border-red-500", className)} {...props} />
      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  );
}
