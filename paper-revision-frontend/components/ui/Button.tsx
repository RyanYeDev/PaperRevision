"use client";
import { cn } from "@/lib/utils";
import { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "secondary" | "danger" | "ghost";
type Size = "sm" | "md" | "lg";

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
}

const styles: Record<Variant, string> = {
  primary: "bg-blue-600 text-white hover:bg-blue-700 border-blue-600",
  secondary: "bg-white text-gray-700 hover:bg-gray-50 border-gray-300",
  danger: "bg-red-600 text-white hover:bg-red-700 border-red-600",
  ghost: "bg-transparent text-gray-600 hover:bg-gray-100 border-transparent",
};

const sizes: Record<Size, string> = {
  sm: "px-3 py-1.5 text-xs",
  md: "px-4 py-2 text-sm",
  lg: "px-6 py-3 text-base",
};

export function Button({ variant = "primary", size = "md", loading, className, children, disabled, ...props }: Props) {
  return (
    <button
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-lg border font-medium transition-colors disabled:opacity-50",
        styles[variant], sizes[size], className
      )}
      disabled={disabled || loading}
      {...props}
    >
      {loading && <span className="animate-spin text-xs">&#9696;</span>}
      {children}
    </button>
  );
}
