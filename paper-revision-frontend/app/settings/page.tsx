"use client";

import { useState } from "react";
import { api } from "@/lib/api";

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<"providers" | "agents">("providers");

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">系统设置</h2>

      <div className="flex gap-4 mb-6">
        {[
          { key: "providers" as const, label: "LLM提供商" },
          { key: "agents" as const, label: "Agent配置" },
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
    </div>
  );
}

function ProviderSettings() {
  const [name, setName] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [providerType, setProviderType] = useState("CUSTOM");

  async function handleCreate() {
    await api.llm.create({ name, providerType, baseUrl, apiKey });
    alert("提供商创建成功");
  }

  return (
    <div className="p-6 bg-white rounded-lg border max-w-lg">
      <h3 className="font-semibold mb-4">添加LLM提供商</h3>
      <div className="space-y-3">
        <div>
          <label className="block text-sm mb-1">提供商类型</label>
          <select value={providerType} onChange={(e) => setProviderType(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm">
            <option value="DEEPSEEK">DeepSeek</option>
            <option value="DOUBAO">豆包 (Doubao)</option>
            <option value="OPENAI">OpenAI</option>
            <option value="CUSTOM">自定义</option>
          </select>
        </div>
        <div>
          <label className="block text-sm mb-1">名称</label>
          <input value={name} onChange={(e) => setName(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" placeholder="DeepSeek / 豆包" />
        </div>
        <div>
          <label className="block text-sm mb-1">API Base URL</label>
          <input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" placeholder="https://api.deepseek.com" />
        </div>
        <div>
          <label className="block text-sm mb-1">API Key</label>
          <input type="password" value={apiKey} onChange={(e) => setApiKey(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" placeholder="sk-..." />
        </div>
        <button onClick={handleCreate}
          className="w-full py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          添加提供商
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
