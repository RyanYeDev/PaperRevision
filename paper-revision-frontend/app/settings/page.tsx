"use client";

import { useState, useEffect } from "react";
import { api, API_BASE } from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<"providers" | "agents" | "grobid">("providers");

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">系统设置</h2>

      <div className="flex gap-4 mb-6">
        {[
          { key: "providers" as const, label: "LLM提供商" },
          { key: "agents" as const, label: "Agent配置" },
          { key: "grobid" as const, label: "GROBID模型" },
        ].map((tab) => (
          <button key={tab.key} onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2 rounded-lg text-sm font-medium ${
              activeTab === tab.key ? "bg-blue-600 text-white" : "bg-white border text-gray-600"}`}>
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "providers" && <ProviderSettings />}
      {activeTab === "agents" && <AgentSettings />}
      {activeTab === "grobid" && <GrobidSettings />}
    </div>
  );
}

function ProviderSettings() {
  const PRESETS: Record<string, { name: string; baseUrl: string; model: string; desc: string }> = {
    DEEPSEEK: { name: "DeepSeek", baseUrl: "https://api.deepseek.com", model: "deepseek-chat", desc: "DeepSeek深度求索" },
    DOUBAO: { name: "豆包(Doubao)", baseUrl: "https://ark.cn-beijing.volces.com/api/v3", model: "doubao-pro-32k", desc: "字节跳动豆包大模型" },
    OPENAI: { name: "OpenAI", baseUrl: "https://api.openai.com", model: "gpt-4o", desc: "OpenAI GPT系列" },
    CUSTOM: { name: "", baseUrl: "", model: "", desc: "自定义兼容OpenAI接口的模型" },
  };
  const [providerType, setProviderType] = useState("DEEPSEEK");
  const [name, setName] = useState(PRESETS.DEEPSEEK.name);
  const [baseUrl, setBaseUrl] = useState(PRESETS.DEEPSEEK.baseUrl);
  const [model, setModel] = useState(PRESETS.DEEPSEEK.model);
  const [apiKey, setApiKey] = useState("");
  const [testResult, setTestResult] = useState<string | null>(null);

  function selectProvider(type: string) {
    setProviderType(type);
    setName(PRESETS[type].name);
    setBaseUrl(PRESETS[type].baseUrl);
    setModel(PRESETS[type].model);
    setTestResult(null);
  }

  async function handleCreate() {
    await api.llm.create({ name, providerType, baseUrl, apiKey, defaultModel: model });
    alert("提供商创建成功");
  }

  return (
    <div className="space-y-4 max-w-lg">
      {/* Preset cards */}
      <div className="grid grid-cols-2 gap-3">
        {Object.entries(PRESETS).filter(([k]) => k !== "CUSTOM").map(([key, preset]) => (
          <button key={key} onClick={() => selectProvider(key)}
            className="paper-card p-4 text-left transition-all"
            style={{ borderColor: providerType === key ? 'var(--primary)' : 'var(--border)', borderWidth: providerType === key ? '2px' : '1.5px' }}>
            <div className="text-lg font-bold">{preset.name}</div>
            <div className="text-xs mt-1" style={{ color: 'var(--text-light)' }}>{preset.desc}</div>
            <div className="text-xs mt-1" style={{ color: 'var(--text-light)' }}>{preset.model}</div>
          </button>
        ))}
      </div>

      {/* Config form */}
      <div className="paper-card p-5 space-y-3">
        <h3 className="font-bold text-lg" style={{ color: 'var(--primary)' }}>{providerType === "CUSTOM" ? "自定义配置" : `配置 ${name}`}</h3>
        <div>
          <label className="block text-sm font-medium mb-1">API Base URL</label>
          <input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)}
            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2" style={{ borderColor: 'var(--border)' }} />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">模型名称</label>
          <input value={model} onChange={(e) => setModel(e.target.value)}
            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2" style={{ borderColor: 'var(--border)' }} />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">API Key</label>
          <input type="password" value={apiKey} onChange={(e) => setApiKey(e.target.value)}
            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2" style={{ borderColor: 'var(--border)' }} placeholder="sk-..." />
        </div>
        {testResult && (
          <div className={`p-3 rounded-lg text-sm ${testResult.includes("成功") ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"}`}>
            {testResult}
          </div>
        )}
        <button onClick={handleCreate}
          className="w-full py-2.5 text-white rounded-xl font-bold btn-cartoon" style={{ background: 'var(--primary)' }}>
          保存提供商
        </button>
      </div>
    </div>
  );
}

function AgentSettings() {
  const [name, setName] = useState("");
  const [systemPrompt, setSystemPrompt] = useState("");

  async function handleCreate() {
    await api.agents.create({ name, systemPrompt, modelProvider: "DEEPSEEK", modelName: "deepseek-chat" });
    alert("Agent创建成功");
  }

  return (
    <div className="p-6 bg-white rounded-lg border max-w-lg">
      <h3 className="font-semibold mb-4">创建Agent</h3>
      <div className="space-y-3">
        <div>
          <label className="block text-sm mb-1">Agent名称</label>
          <input value={name} onChange={(e) => setName(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" placeholder="论文返修助手" />
        </div>
        <div>
          <label className="block text-sm mb-1">System Prompt</label>
          <textarea value={systemPrompt} onChange={(e) => setSystemPrompt(e.target.value)} rows={5}
            className="w-full border rounded px-3 py-2 text-sm" placeholder="你是一个专业的论文返修助手..." />
        </div>
        <button onClick={handleCreate}
          className="w-full py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          创建Agent
        </button>
      </div>
    </div>
  );
}

function GrobidSettings() {
  const { token } = useAuth();
  const [status, setStatus] = useState<{ installed: boolean; message: string; downloadUrl: string; downloadSize: string } | null>(null);
  const [uploading, setUploading] = useState(false);

  const headers = { Authorization: `Bearer ${token}`, "Content-Type": "application/json" };

  useEffect(() => {
    if (!token) return;
    fetch(`${API_BASE}/grobid/status`, { headers })
      .then(r => r.json())
      .then(d => setStatus(d.data))
      .catch(() => {});
  }, [token]);

  async function handleUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    const formData = new FormData();
    formData.append("file", file);
    try {
      await fetch(`${API_BASE}/grobid/models/upload`, { method: "POST", body: formData });
      const r = await fetch(`${API_BASE}/grobid/status`);
      const d = await r.json();
      setStatus(d.data);
      alert("模型安装成功！");
    } catch (err) {
      alert("安装失败: " + err);
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="p-6 bg-white rounded-lg border max-w-lg space-y-4">
      <h3 className="font-semibold text-lg">GROBID 模型管理</h3>

      {/* 状态 */}
      <div className={`p-4 rounded-lg ${status?.installed ? "bg-green-50 border border-green-200" : "bg-yellow-50 border border-yellow-200"}`}>
        <div className="flex items-center gap-2 mb-2">
          <span className={`w-3 h-3 rounded-full ${status?.installed ? "bg-green-500" : "bg-yellow-500"}`} />
          <span className="font-medium text-sm">{status?.installed ? "模型已安装" : "模型未安装"}</span>
        </div>
        <p className="text-xs text-gray-600">{status?.message}</p>
      </div>

      {/* 安装方式 */}
      {!status?.installed && (
        <div className="space-y-3">
          <div className="p-3 bg-blue-50 rounded-lg border border-blue-100">
            <p className="text-sm font-medium text-blue-800 mb-2">方式1: 上传模型包（推荐）</p>
            <p className="text-xs text-blue-600 mb-3">
              下载模型压缩包（{status?.downloadSize || "约1.2GB"}），然后在此上传
            </p>
            <a href={status?.downloadUrl || "#"} target="_blank"
              className="text-xs text-blue-600 underline hover:text-blue-800 mb-3 block">
              点击下载模型包 →
            </a>
            <label className="inline-block px-4 py-2 bg-blue-600 text-white text-sm rounded-lg cursor-pointer hover:bg-blue-700">
              {uploading ? "安装中..." : "上传模型zip"}
              <input type="file" accept=".zip" onChange={handleUpload} className="hidden" disabled={uploading} />
            </label>
          </div>

          <div className="p-3 bg-gray-50 rounded-lg border">
            <p className="text-sm font-medium text-gray-700 mb-1">方式2: 手动安装</p>
            <p className="text-xs text-gray-500">
              下载模型包后，解压到项目目录:<br/>
              <code className="bg-gray-200 px-1 rounded text-xs">
                paper-revision-backend/data/grobid-home/models/
              </code>
            </p>
          </div>

          <div className="p-3 bg-gray-50 rounded-lg border">
            <p className="text-sm font-medium text-gray-700 mb-1">💡 没有模型也能用</p>
            <p className="text-xs text-gray-500">
              GROBID模型未安装时会自动使用 PDFBox 坐标分析引擎，
              支持单双栏检测、标题识别、表格提取、公式标注，不影响正常使用。
            </p>
          </div>
        </div>
      )}

      {/* 已安装：卸载按钮 */}
      {status?.installed && (
        <button onClick={async () => {
          await fetch(`${API_BASE}/grobid/models`, { method: "DELETE" });
          const r = await fetch(`${API_BASE}/grobid/status`);
          setStatus((await r.json()).data);
        }}
          className="px-4 py-2 text-sm text-red-600 border border-red-200 rounded-lg hover:bg-red-50">
          卸载模型
        </button>
      )}
    </div>
  );
}
