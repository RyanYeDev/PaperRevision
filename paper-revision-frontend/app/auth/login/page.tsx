"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Card, CardHeader, CardContent } from "@/components/ui/Card";

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(""); setLoading(true);
    try {
      await login(email, password);
      router.push("/papers");
    } catch (err: any) {
      setError(err.message || "登录失败");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-md mx-auto mt-16">
      <Card>
        <CardHeader><h2 className="text-xl font-display font-bold text-center" style={{ color: "var(--text)" }}>登录 PaperRevision</h2></CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {error && <div className="p-3 rounded-soft text-sm border" style={{ background: "var(--danger-tint)", borderColor: "var(--danger)", color: "#b45454" }}>{error}</div>}
            <Input label="邮箱" type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="your@email.com" required />
            <Input label="密码" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="******" required />
            <Button type="submit" loading={loading} className="w-full">登录</Button>
          </form>
          <p className="text-center text-sm mt-4" style={{ color: "var(--text-light)" }}>
            还没有账号？<a href="/auth/register" className="font-bold hover:underline" style={{ color: "var(--primary-deep)" }}>注册</a>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
