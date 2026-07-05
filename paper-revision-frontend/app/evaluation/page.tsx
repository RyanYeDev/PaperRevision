"use client";

import { useState, useEffect } from "react";
import { api } from "@/lib/api";

type Tab = "dashboard" | "test-cases" | "test-suites";

interface TestCase {
  id: string; name: string; description: string;
  inputData: string; expectedOutput: string;
  sourceDataset: string; difficulty: string;
  createdAt: string;
}

interface TestSuite {
  id: string; name: string; description: string;
  status: string; createdAt: string;
}

interface EvalHistoryItem {
  id: string; revisionResultId: string;
  relevanceScore: number; faithfulnessScore: number;
  completenessScore: number; formatScore: number;
  overallScore: number; feedback: string;
  evaluatorType: string; grade: string; createdAt: string;
}

interface TrajectoryMetrics {
  stepEfficiency: number; toolAccuracy: number;
  paramCorrectness: number; redundancyRate: number;
  totalDurationMs: number; totalTokens: number;
  successRate: number; totalModelCalls: number;
  totalSteps: number; successSteps: number;
}

export default function EvaluationPage() {
  const [activeTab, setActiveTab] = useState<Tab>("dashboard");
  const [testCases, setTestCases] = useState<TestCase[]>([]);
  const [testSuites, setTestSuites] = useState<TestSuite[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  // Form states
  const [caseName, setCaseName] = useState("");
  const [caseDesc, setCaseDesc] = useState("");
  const [caseInput, setCaseInput] = useState("");
  const [caseExpected, setCaseExpected] = useState("");
  const [caseDataset, setCaseDataset] = useState("CUSTOM");
  const [caseDifficulty, setCaseDifficulty] = useState("MEDIUM");

  const [suiteName, setSuiteName] = useState("");
  const [suiteDesc, setSuiteDesc] = useState("");

  const [trajectorySessionId, setTrajectorySessionId] = useState("");
  const [trajectoryResult, setTrajectoryResult] = useState<TrajectoryMetrics | null>(null);
  const [evalHistory, setEvalHistory] = useState<EvalHistoryItem[]>([]);
  const [historyResultId, setHistoryResultId] = useState("");

  useEffect(() => {
    if (activeTab === "test-cases") loadTestCases();
    if (activeTab === "test-suites") loadTestSuites();
  }, [activeTab]);

  async function loadTestCases() {
    setLoading(true);
    try {
      const res = await api.testCases.list() as unknown as { code: number; data: TestCase[] };
      setTestCases(res.data || []);
    } catch { setMessage("加载测试用例失败"); }
    setLoading(false);
  }

  async function loadTestSuites() {
    setLoading(true);
    try {
      const res = await api.testSuites.list() as unknown as { code: number; data: TestSuite[] };
      setTestSuites(res.data || []);
    } catch { setMessage("加载测试套件失败"); }
    setLoading(false);
  }

  async function handleCreateCase() {
    if (!caseName || !caseInput) { setMessage("名称和输入为必填"); return; }
    setLoading(true);
    try {
      await api.testCases.create({
        name: caseName, description: caseDesc,
        inputData: caseInput, expectedOutput: caseExpected,
        sourceDataset: caseDataset, difficulty: caseDifficulty,
      });
      setMessage("测试用例创建成功");
      setCaseName(""); setCaseDesc(""); setCaseInput(""); setCaseExpected("");
      loadTestCases();
    } catch { setMessage("创建失败"); }
    setLoading(false);
  }

  async function handleDeleteCase(id: string) {
    if (!confirm("确定删除？")) return;
    try { await api.testCases.delete(id); loadTestCases(); } catch { setMessage("删除失败"); }
  }

  async function handleCreateSuite() {
    if (!suiteName) { setMessage("名称为必填"); return; }
    setLoading(true);
    try {
      await api.testSuites.create({ name: suiteName, description: suiteDesc });
      setMessage("测试套件创建成功");
      setSuiteName(""); setSuiteDesc("");
      loadTestSuites();
    } catch { setMessage("创建失败"); }
    setLoading(false);
  }

  async function handleRunSuite(id: string) {
    setLoading(true);
    try {
      const res = await api.testSuites.run(id) as unknown as { code: number; data: { id: string; passedCases: number; totalCases: number } };
      setMessage(`套件执行完成: ${res.data.passedCases}/${res.data.totalCases} 通过`);
      loadTestSuites();
    } catch { setMessage("执行失败"); }
    setLoading(false);
  }

  async function handleTrajectoryQuery() {
    if (!trajectorySessionId) return;
    setLoading(true);
    try {
      const res = await api.evaluation.trajectory(trajectorySessionId) as unknown as { code: number; data: TrajectoryMetrics };
      setTrajectoryResult(res.data);
    } catch { setMessage("查询失败"); }
    setLoading(false);
  }

  async function handleHistoryQuery() {
    if (!historyResultId) return;
    setLoading(true);
    try {
      const res = await api.evaluation.history(historyResultId) as unknown as { code: number; data: EvalHistoryItem[] };
      setEvalHistory(res.data || []);
    } catch { setMessage("查询失败"); }
    setLoading(false);
  }

  const tabs: { key: Tab; label: string; icon: string }[] = [
    { key: "dashboard", label: "评估仪表盘", icon: "📊" },
    { key: "test-cases", label: "测试用例", icon: "📋" },
    { key: "test-suites", label: "测试套件", icon: "🧪" },
  ];

  return (
    <div className="max-w-6xl mx-auto">
      <h1 className="text-2xl font-bold mb-6" style={{ color: 'var(--text)' }}>
        🤖 Agent 评估中心
      </h1>

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        {tabs.map(t => (
          <button key={t.key}
            onClick={() => setActiveTab(t.key)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              activeTab === t.key
                ? "text-white"
                : "bg-white border text-gray-600 hover:bg-gray-50"
            }`}
            style={activeTab === t.key ? { background: 'var(--primary)' } : { borderColor: 'var(--border)' }}
          >
            {t.icon} {t.label}
          </button>
        ))}
      </div>

      {message && (
        <div className={`mb-4 p-3 rounded-lg text-sm ${message.includes("失败") ? "bg-red-50 border border-red-200 text-red-600" : "bg-green-50 border border-green-200 text-green-700"}`}>
          {message}
          <button className="ml-3 underline" onClick={() => setMessage("")}>关闭</button>
        </div>
      )}

      {/* ======== DASHBOARD TAB ======== */}
      {activeTab === "dashboard" && <DashboardTab
        trajectorySessionId={trajectorySessionId} setTrajectorySessionId={setTrajectorySessionId}
        trajectoryResult={trajectoryResult} handleTrajectoryQuery={handleTrajectoryQuery}
        historyResultId={historyResultId} setHistoryResultId={setHistoryResultId}
        evalHistory={evalHistory} handleHistoryQuery={handleHistoryQuery}
        loading={loading}
      />}

      {/* ======== TEST CASES TAB ======== */}
      {activeTab === "test-cases" && <TestCasesTab
        testCases={testCases} loading={loading}
        caseName={caseName} setCaseName={setCaseName}
        caseDesc={caseDesc} setCaseDesc={setCaseDesc}
        caseInput={caseInput} setCaseInput={setCaseInput}
        caseExpected={caseExpected} setCaseExpected={setCaseExpected}
        caseDataset={caseDataset} setCaseDataset={setCaseDataset}
        caseDifficulty={caseDifficulty} setCaseDifficulty={setCaseDifficulty}
        handleCreateCase={handleCreateCase} handleDeleteCase={handleDeleteCase}
      />}

      {/* ======== TEST SUITES TAB ======== */}
      {activeTab === "test-suites" && <TestSuitesTab
        testSuites={testSuites} testCases={testCases} loading={loading}
        suiteName={suiteName} setSuiteName={setSuiteName}
        suiteDesc={suiteDesc} setSuiteDesc={setSuiteDesc}
        handleCreateSuite={handleCreateSuite} handleRunSuite={handleRunSuite}
      />}
    </div>
  );
}

/* ==================== DASHBOARD SUB-COMPONENT ==================== */
function DashboardTab({
  trajectorySessionId, setTrajectorySessionId, trajectoryResult, handleTrajectoryQuery,
  historyResultId, setHistoryResultId, evalHistory, handleHistoryQuery, loading,
}: {
  trajectorySessionId: string; setTrajectorySessionId: (v: string) => void;
  trajectoryResult: TrajectoryMetrics | null; handleTrajectoryQuery: () => void;
  historyResultId: string; setHistoryResultId: (v: string) => void;
  evalHistory: EvalHistoryItem[]; handleHistoryQuery: () => void; loading: boolean;
}) {
  const metrics = trajectoryResult ? [
    { label: "步骤效率", value: (trajectoryResult.stepEfficiency * 100).toFixed(0) + "%", desc: "预期/实际步骤比" },
    { label: "工具准确率", value: (trajectoryResult.toolAccuracy * 100).toFixed(0) + "%", desc: "正确工具选择占比" },
    { label: "参数正确率", value: (trajectoryResult.paramCorrectness * 100).toFixed(0) + "%", desc: "参数无误调用占比" },
    { label: "冗余调用率", value: (trajectoryResult.redundancyRate * 100).toFixed(0) + "%", desc: "无效/重复调用占比" },
    { label: "端到端延迟", value: trajectoryResult.totalDurationMs + "ms", desc: "所有步骤总耗时" },
    { label: "Token 消耗", value: String(trajectoryResult.totalTokens), desc: "模型调用总 Token" },
    { label: "成功率", value: (trajectoryResult.successRate * 100).toFixed(0) + "%", desc: "SUCCESS 步骤占比" },
    { label: "总步骤数", value: String(trajectoryResult.totalSteps), desc: "成功 " + trajectoryResult.successSteps + " 步" },
  ] : [];

  return (
    <div className="space-y-6">
      {/* Trajectory Query */}
      <div className="paper-card p-6">
        <h2 className="text-lg font-bold mb-4" style={{ color: 'var(--text)' }}>🔍 轨迹分析查询</h2>
        <div className="flex gap-3">
          <input
            className="flex-1 border rounded-xl px-3 py-2 text-sm"
            style={{ borderColor: 'var(--border)' }}
            placeholder="输入 Session ID (traceId)"
            value={trajectorySessionId}
            onChange={e => setTrajectorySessionId(e.target.value)}
            onKeyDown={e => e.key === "Enter" && handleTrajectoryQuery()}
          />
          <button className="btn-cartoon px-4 py-2 text-white rounded-xl text-sm font-medium"
            style={{ background: 'var(--primary)' }}
            onClick={handleTrajectoryQuery} disabled={loading}>
            {loading ? "查询中..." : "查询"}
          </button>
        </div>

        {trajectoryResult && (
          <div className="mt-4 grid grid-cols-4 gap-3">
            {metrics.map(m => (
              <div key={m.label} className="text-center p-3 bg-gray-50 rounded-lg">
                <div className="text-xl font-bold" style={{ color: 'var(--primary)' }}>{m.value}</div>
                <div className="text-xs font-medium" style={{ color: 'var(--text)' }}>{m.label}</div>
                <div className="text-xs mt-1" style={{ color: 'var(--text-light)' }}>{m.desc}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* History Query */}
      <div className="paper-card p-6">
        <h2 className="text-lg font-bold mb-4" style={{ color: 'var(--text)' }}>📜 评估历史查询</h2>
        <div className="flex gap-3 mb-4">
          <input
            className="flex-1 border rounded-xl px-3 py-2 text-sm"
            style={{ borderColor: 'var(--border)' }}
            placeholder="输入 Revision Result ID"
            value={historyResultId}
            onChange={e => setHistoryResultId(e.target.value)}
            onKeyDown={e => e.key === "Enter" && handleHistoryQuery()}
          />
          <button className="btn-cartoon px-4 py-2 text-white rounded-xl text-sm font-medium"
            style={{ background: 'var(--primary)' }}
            onClick={handleHistoryQuery} disabled={loading}>
            {loading ? "查询中..." : "查询"}
          </button>
        </div>

        {evalHistory.length > 0 && (
          <div className="space-y-3">
            {evalHistory.map((e, i) => (
              <div key={i} className="p-4 bg-white border rounded-lg" style={{ borderColor: 'var(--border)' }}>
                <div className="flex gap-3 mb-2">
                  {[
                    { k: "相关性", v: e.relevanceScore },
                    { k: "忠实度", v: e.faithfulnessScore },
                    { k: "完整性", v: e.completenessScore },
                    { k: "格式", v: e.formatScore },
                  ].map(d => (
                    <div key={d.k} className="flex-1 text-center">
                      <div className="text-lg font-bold" style={{ color: 'var(--primary)' }}>
                        {((d.v || 0) * 100).toFixed(0)}%
                      </div>
                      <div className="text-xs" style={{ color: 'var(--text-light)' }}>{d.k}</div>
                    </div>
                  ))}
                </div>
                <div className="flex justify-between items-center pt-2 border-t text-xs" style={{ borderColor: 'var(--border)' }}>
                  <span style={{ color: 'var(--text)' }}>
                    综合: <b>{((e.overallScore || 0) * 100).toFixed(0)}%</b> · {e.grade}
                    <span className="ml-3 px-1.5 py-0.5 rounded text-xs"
                      style={{ background: e.evaluatorType === "LLM_JUDGE" ? '#e0f2fe' : '#fef9c3', color: 'var(--text)' }}>
                      {e.evaluatorType}
                    </span>
                  </span>
                  <span style={{ color: 'var(--text-light)' }}>{e.createdAt}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

/* ==================== TEST CASES SUB-COMPONENT ==================== */
function TestCasesTab({
  testCases, loading, caseName, setCaseName, caseDesc, setCaseDesc,
  caseInput, setCaseInput, caseExpected, setCaseExpected,
  caseDataset, setCaseDataset, caseDifficulty, setCaseDifficulty,
  handleCreateCase, handleDeleteCase,
}: {
  testCases: TestCase[]; loading: boolean;
  caseName: string; setCaseName: (v: string) => void;
  caseDesc: string; setCaseDesc: (v: string) => void;
  caseInput: string; setCaseInput: (v: string) => void;
  caseExpected: string; setCaseExpected: (v: string) => void;
  caseDataset: string; setCaseDataset: (v: string) => void;
  caseDifficulty: string; setCaseDifficulty: (v: string) => void;
  handleCreateCase: () => void; handleDeleteCase: (id: string) => void;
}) {
  return (
    <div className="space-y-6">
      {/* Create Form */}
      <div className="paper-card p-6">
        <h2 className="text-lg font-bold mb-4" style={{ color: 'var(--text)' }}>➕ 新建测试用例</h2>
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--text)' }}>名称 *</label>
            <input className="w-full border rounded-xl px-3 py-2 text-sm" style={{ borderColor: 'var(--border)' }}
              value={caseName} onChange={e => setCaseName(e.target.value)} placeholder="如：改进摘要任务" />
          </div>
          <div>
            <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--text)' }}>来源数据集</label>
            <select className="w-full border rounded-xl px-3 py-2 text-sm" style={{ borderColor: 'var(--border)' }}
              value={caseDataset} onChange={e => setCaseDataset(e.target.value)}>
              <option value="CUSTOM">自定义</option>
              <option value="GAIA">GAIA</option>
              <option value="HOTPOTQA">HotPotQA</option>
              <option value="SWE_BENCH">SWE-bench</option>
            </select>
          </div>
          <div>
            <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--text)' }}>难度</label>
            <select className="w-full border rounded-xl px-3 py-2 text-sm" style={{ borderColor: 'var(--border)' }}
              value={caseDifficulty} onChange={e => setCaseDifficulty(e.target.value)}>
              <option value="EASY">简单</option>
              <option value="MEDIUM">中等</option>
              <option value="HARD">困难</option>
            </select>
          </div>
          <div>
            <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--text)' }}>描述</label>
            <input className="w-full border rounded-xl px-3 py-2 text-sm" style={{ borderColor: 'var(--border)' }}
              value={caseDesc} onChange={e => setCaseDesc(e.target.value)} placeholder="简要描述测试目的" />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--text)' }}>输入数据 (JSON) *</label>
            <textarea className="w-full border rounded-xl px-3 py-2 text-sm h-24 resize-y" style={{ borderColor: 'var(--border)' }}
              value={caseInput} onChange={e => setCaseInput(e.target.value)}
              placeholder='{"paperId": "...", "revisionComments": ["修改摘要"]}' />
          </div>
          <div>
            <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--text)' }}>期望输出</label>
            <textarea className="w-full border rounded-xl px-3 py-2 text-sm h-24 resize-y" style={{ borderColor: 'var(--border)' }}
              value={caseExpected} onChange={e => setCaseExpected(e.target.value)}
              placeholder="期望的修改结果..." />
          </div>
        </div>
        <button className="btn-cartoon px-6 py-2 text-white rounded-xl text-sm font-medium"
          style={{ background: 'var(--primary)' }}
          onClick={handleCreateCase} disabled={loading}>
          {loading ? "创建中..." : "创建用例"}
        </button>
      </div>

      {/* List */}
      <div className="paper-card p-6">
        <h2 className="text-lg font-bold mb-4" style={{ color: 'var(--text)' }}>📋 测试用例列表 ({testCases.length})</h2>
        {testCases.length === 0 ? (
          <div className="text-center py-8" style={{ color: 'var(--text-light)' }}>暂无测试用例，请创建</div>
        ) : (
          <div className="space-y-2">
            {testCases.map(tc => (
              <div key={tc.id} className="flex items-center justify-between p-3 bg-white border rounded-lg hover:bg-gray-50 transition-colors"
                style={{ borderColor: 'var(--border)' }}>
                <div className="flex-1 min-w-0">
                  <div className="font-medium text-sm" style={{ color: 'var(--text)' }}>{tc.name}</div>
                  <div className="text-xs mt-0.5" style={{ color: 'var(--text-light)' }}>
                    {tc.sourceDataset} · {tc.difficulty} · {tc.createdAt?.slice(0, 10)}
                  </div>
                </div>
                <button className="text-xs px-3 py-1 rounded-lg text-red-600 hover:bg-red-50 transition-colors"
                  onClick={() => handleDeleteCase(tc.id)}>删除</button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

/* ==================== TEST SUITES SUB-COMPONENT ==================== */
function TestSuitesTab({
  testSuites, testCases, loading, suiteName, setSuiteName,
  suiteDesc, setSuiteDesc, handleCreateSuite, handleRunSuite,
}: {
  testSuites: TestSuite[]; testCases: TestCase[]; loading: boolean;
  suiteName: string; setSuiteName: (v: string) => void;
  suiteDesc: string; setSuiteDesc: (v: string) => void;
  handleCreateSuite: () => void; handleRunSuite: (id: string) => void;
}) {
  return (
    <div className="space-y-6">
      {/* Create Form */}
      <div className="paper-card p-6">
        <h2 className="text-lg font-bold mb-4" style={{ color: 'var(--text)' }}>🧪 新建测试套件</h2>
        <div className="flex gap-4 mb-4">
          <div className="flex-1">
            <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--text)' }}>名称 *</label>
            <input className="w-full border rounded-xl px-3 py-2 text-sm" style={{ borderColor: 'var(--border)' }}
              value={suiteName} onChange={e => setSuiteName(e.target.value)}
              placeholder="如：论文返修回归测试套件" />
          </div>
          <div className="flex-1">
            <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--text)' }}>描述</label>
            <input className="w-full border rounded-xl px-3 py-2 text-sm" style={{ borderColor: 'var(--border)' }}
              value={suiteDesc} onChange={e => setSuiteDesc(e.target.value)}
              placeholder="测试套件的用途说明" />
          </div>
        </div>
        <button className="btn-cartoon px-6 py-2 text-white rounded-xl text-sm font-medium"
          style={{ background: 'var(--primary)' }}
          onClick={handleCreateSuite} disabled={loading}>
          {loading ? "创建中..." : "创建套件"}
        </button>
      </div>

      {/* List */}
      <div className="paper-card p-6">
        <h2 className="text-lg font-bold mb-4" style={{ color: 'var(--text)' }}>📦 测试套件列表 ({testSuites.length})</h2>
        {testSuites.length === 0 ? (
          <div className="text-center py-8" style={{ color: 'var(--text-light)' }}>暂无测试套件，请创建</div>
        ) : (
          <div className="space-y-3">
            {testSuites.map(suite => (
              <div key={suite.id} className="p-4 bg-white border rounded-lg" style={{ borderColor: 'var(--border)' }}>
                <div className="flex items-center justify-between mb-2">
                  <div>
                    <span className="font-medium text-sm" style={{ color: 'var(--text)' }}>{suite.name}</span>
                    <span className={`ml-2 px-2 py-0.5 rounded-full text-xs ${
                      suite.status === "COMPLETED" ? "bg-green-100 text-green-700"
                      : suite.status === "RUNNING" ? "bg-blue-100 text-blue-700"
                      : suite.status === "READY" ? "bg-yellow-100 text-yellow-700"
                      : "bg-gray-100 text-gray-600"
                    }`}>{suite.status}</span>
                  </div>
                  <div className="flex gap-2">
                    <button className="btn-cartoon px-3 py-1 text-white rounded-lg text-xs"
                      style={{ background: 'var(--accent)', color: '#7a6200' }}
                      onClick={() => handleRunSuite(suite.id)} disabled={loading}>
                      ▶ 执行
                    </button>
                  </div>
                </div>
                <div className="text-xs" style={{ color: 'var(--text-light)' }}>
                  {suite.description || "无描述"} · 创建于 {suite.createdAt?.slice(0, 10)}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
