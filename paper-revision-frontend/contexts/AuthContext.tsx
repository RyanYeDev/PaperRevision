"use client";
import { createContext, useContext, useState, useEffect, useCallback } from "react";
import { API_BASE } from "@/lib/api";

interface User { id: string; nickname: string; email: string; }
interface AuthCtx {
  user: User | null; token: string | null; loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, nickname: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthCtx>({ user: null, token: null, loading: true, login: async () => {}, register: async () => {}, logout: () => {} });

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const saved = localStorage.getItem("token");
    if (saved) {
      setToken(saved);
      fetch(`${API_BASE}/auth/me`, { headers: { Authorization: `Bearer ${saved}` } })
        .then(r => r.json()).then(d => { if (d.code === 200) setUser(d.data); })
        .finally(() => setLoading(false));
    } else { setLoading(false); }
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await fetch(`${API_BASE}/auth/login`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password }) });
    const d = await res.json();
    if (d.code !== 200) throw new Error(d.message);
    localStorage.setItem("token", d.data.token);
    setToken(d.data.token);
    setUser(d.data.user);
  }, []);

  const register = useCallback(async (email: string, nickname: string, password: string) => {
    const res = await fetch(`${API_BASE}/auth/register`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, nickname, password }) });
    const d = await res.json();
    if (d.code !== 200) throw new Error(d.message);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("token");
    setToken(null); setUser(null);
  }, []);

  return <AuthContext.Provider value={{ user, token, loading, login, register, logout }}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
