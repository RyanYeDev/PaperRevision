import type { Config } from "tailwindcss";

// 设计 token 接入 Tailwind —— 与 globals.css 的 :root 变量保持一致，
// 这样既能写 bg-primary / text-accent 等语义类，也能继续用 CSS 变量。
const config: Config = {
  content: ["./app/**/*.{js,ts,jsx,tsx,mdx}", "./components/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: "var(--primary)",
          deep: "var(--primary-deep)",
          soft: "var(--primary-soft)",
          tint: "var(--primary-tint)",
        },
        accent: {
          DEFAULT: "var(--accent)",
          deep: "var(--accent-deep)",
          ink: "var(--accent-ink)",
        },
        success: "var(--success)",
        danger: "var(--danger)",
        ink: "var(--text)",
        muted: "var(--text-light)",
        card: "var(--card)",
        line: "var(--border)",
      },
      borderRadius: {
        card: "var(--radius)",
        soft: "var(--radius-sm)",
        xl2: "var(--radius-lg)",
      },
      fontFamily: {
        display: "var(--font-display)",
        body: "var(--font-body)",
      },
      boxShadow: {
        soft: "var(--shadow-sm)",
        card: "var(--shadow-md)",
        primary: "var(--shadow-primary)",
      },
      keyframes: {
        "reveal-up": {
          from: { opacity: "0", transform: "translateY(24px)" },
          to: { opacity: "1", transform: "translateY(0)" },
        },
      },
      animation: {
        "reveal-up": "reveal-up 0.7s cubic-bezier(0.16,1,0.3,1) forwards",
      },
    },
  },
  plugins: [],
};
export default config;
