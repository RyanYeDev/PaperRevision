const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8088/api";

class HttpClient {
  private getToken(): string | null {
    if (typeof window !== "undefined") {
      return localStorage.getItem("token");
    }
    return null;
  }

  private async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const token = this.getToken();
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...((token ? { Authorization: `Bearer ${token}` } : {}) as Record<string, string>),
      ...((options.headers as Record<string, string>) || {}),
    };

    const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
    if (!res.ok) throw new Error(`API Error: ${res.status}`);
    return res.json();
  }

  get<T>(path: string) { return this.request<T>(path); }
  post<T>(path: string, data?: unknown) {
    return this.request<T>(path, { method: "POST", body: JSON.stringify(data) });
  }
  put<T>(path: string, data?: unknown) {
    return this.request<T>(path, { method: "PUT", body: JSON.stringify(data) });
  }
  delete<T>(path: string) {
    return this.request<T>(path, { method: "DELETE" });
  }
}

export const httpClient = new HttpClient();

// API methods
export const api = {
  auth: {
    login: (email: string, password: string) =>
      httpClient.post<{ code: number; data: { token: string; user: unknown } }>("/auth/login", { email, password }),
    register: (email: string, nickname: string, password: string) =>
      httpClient.post("/auth/register", { email, nickname, password }),
  },
  papers: {
    upload: async (file: File) => {
      const formData = new FormData();
      formData.append("file", file);
      const token = localStorage.getItem("token");
      const res = await fetch(`${API_BASE}/papers/upload`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      });
      return res.json();
    },
    list: (page = 1) => httpClient.get(`/papers?page=${page}`),
    detail: (id: string) => httpClient.get(`/papers/${id}`),
  },
  revision: {
    execute: (data: { paperId: string; referencePaperId: string; revisionComments: string[] }) =>
      httpClient.post("/revision/execute", data),
    diff: (originalText: string, revisedText: string) =>
      httpClient.post("/revision/diff", { originalText, revisedText }),
    trace: (traceId: string) => httpClient.get(`/revision/trace/${traceId}`),
  },
  llm: {
    list: () => httpClient.get("/llm-providers"),
    create: (data: unknown) => httpClient.post("/llm-providers", data),
  },
  agents: {
    list: () => httpClient.get("/agents"),
    create: (data: unknown) => httpClient.post("/agents", data),
  },
};
