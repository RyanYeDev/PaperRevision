"use client";
import { useState } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";

const steps = [
  { id: "parse", label: "解析返修意见", icon: "📖" },
  { id: "rag", label: "RAG检索参考文献", icon: "🔍" },
  { id: "analyze", label: "逐条分析匹配", icon: "🧠" },
  { id: "generate", label: "生成修改方案", icon: "✨" },
  { id: "execute", label: "执行修改", icon: "🖊️" },
  { id: "check", label: "引用格式检查", icon: "✅" },
];

export default function RevisionPage() {
  const { token } = useAuth();
  const [paperId, setPaperId] = useState("");
  const [refPaperId, setRefPaperId] = useState("");
  const [comments, setComments] = useState("");
  const [loading, setLoading] = useState(false);
  const [activeStep, setActiveStep] = useState(0);
  const [results, setResults] = useState<any>(null);
  const [diff, setDiff] = useState<any[]>([]);
  const [evalScore, setEval] = useState<any>(null);

  async function handleExecute() {
    if (!paperId || !comments.trim()) return;
    setLoading(true); setActiveStep(0); setResults(null); setDiff([]); setEval(null);

    const revisionComments = comments.split("\n").filter(c => c.trim());
    for (let i = 0; i < steps.length; i++) {
      setActiveStep(i);
      await new Promise(r => setTimeout(r, 600));
    }

    try {
      const res: any = await api.revision.execute({ paperId, referencePaperId: refPaperId, revisionComments });
      setResults(res.data?.revisionResults || []);
      setEval(res.data?.evaluation || null);

      if (res.data?.traceId) {
        try {
          const traceRes = await api.revision.trace(res.data.traceId);
          console.log("Trace:", traceRes);
        } catch (_) {}
      }
    } catch (e: any) {
      console.error(e);
    } finally {
      setLoading(false); setActiveStep(0);
    }
  }

  async function handleDiff(idx: number) {
    if (!results?.[idx]) return;
    setDiff([{ type: "REMOVED", content: `[原文] ${results[idx].requirement}` },
             { type: "ADDED", content: `[修改] ${results[idx].suggestedRevision || "待生成"}` }]);
  }

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6" style={{ color: 'var(--primary)' }}>✨ 返修中心</h2>

      <div className="grid grid-cols-2 gap-6">
        {/* Left: Config */}
        <div className="space-y-4">
          <div className="paper-card p-5">
            <h3 className="font-bold text-lg mb-4">📋 返修配置</h3>
            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium mb-1">论文 ID</label>
                <input value={paperId} onChange={e => setPaperId(e.target.value)}
                  className="w-full border rounded-xl px-3 py-2 text-sm" style={{ borderColor: 'var(--border)' }}
                  placeholder="从论文管理页获取论文ID" />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">参考文献 ID</label>
                <input value={refPaperId} onChange={e => setRefPaperId(e.target.value)}
                  className="w-full border rounded-xl px-3 py-2 text-sm" style={{ borderColor: 'var(--border)' }} />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">返修意见 <span className="text-xs" style={{color:'var(--text-light)'}}>(每行一条)</span></label>
                <textarea rows={5} value={comments} onChange={e => setComments(e.target.value)}
                  className="w-full border rounded-xl px-3 py-2 text-sm" style={{ borderColor: 'var(--border)' }}
                  placeholder={"Reviewer建议补充实验对比...\n引言部分需要增加文献综述...\n格式需符合会议模板要求..."} />
              </div>
              <button onClick={handleExecute} disabled={loading || !paperId || !comments.trim()}
                className="w-full py-3 text-white rounded-xl font-bold btn-cartoon disabled:opacity-50"
                style={{ background: 'var(--primary)' }}>
                {loading ? "⏳ 执行中..." : "🚀 开始返修"}
              </button>
            </div>
          </div>

          {/* Workflow progress */}
          {loading && (
            <div className="paper-card p-5">
              <h3 className="font-bold text-sm mb-3">🔄 返修流水线</h3>
              <div className="space-y-2">
                {steps.map((s, i) => (
                  <div key={s.id} className="flex items-center gap-3 text-sm py-1">
                    <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold transition-all
                      ${i < activeStep ? 'text-white' : i === activeStep ? 'animate-pulse text-white' : 'bg-gray-100'}`}
                      style={i <= activeStep ? { background: 'var(--primary)' } : {}}>
                      {i < activeStep ? "✓" : s.icon}
                    </span>
                    <span style={{ color: i <= activeStep ? 'var(--text)' : 'var(--text-light)' }}>{s.label}</span>
                    {i === activeStep && <span className="text-xs animate-pulse" style={{color:'var(--primary)'}}>执行中...</span>}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Right: Results */}
        <div className="space-y-4">
          {evalScore && (
            <div className="paper-card p-5">
              <h3 className="font-bold text-sm mb-3">📊 评估结果</h3>
              <div className="flex gap-3">
                {[
                  { k: "relevanceScore", l: "相关性" },
                  { k: "faithfulnessScore", l: "忠实度" },
                  { k: "completenessScore", l: "完整性" },
                  { k: "formatScore", l: "格式" },
                ].map(m => (
                  <div key={m.k} className="flex-1 text-center">
                    <div className="text-2xl font-bold" style={{ color: 'var(--primary)' }}>
                      {((evalScore[m.k] || 0) * 100).toFixed(0)}%
                    </div>
                    <div className="text-xs" style={{ color: 'var(--text-light)' }}>{m.l}</div>
                  </div>
                ))}
              </div>
              <div className="mt-3 pt-3 border-t text-center text-sm font-bold" style={{ borderColor: 'var(--border)' }}>
                综合: {((evalScore.overallScore || 0) * 100).toFixed(0)}% · {evalScore.grade || "-"}
              </div>
            </div>
          )}

          {(results?.length > 0) && (
            <div className="paper-card p-5">
              <h3 className="font-bold text-sm mb-3">📝 修改建议 ({results.length}条)</h3>
              <div className="space-y-3 max-h-[400px] overflow-auto">
                {results.map((r: any, i: number) => (
                  <div key={i} className="p-3 rounded-xl cursor-pointer hover:opacity-80 transition-opacity"
                    style={{ background: '#f8f6f3' }} onClick={() => handleDiff(i)}>
                    <div className="flex items-start gap-2">
                      <span className="text-xs font-bold px-1.5 py-0.5 rounded-full text-white"
                        style={{ background: 'var(--primary)' }}>#{r.requirementIndex}</span>
                      <div className="flex-1">
                        <p className="text-sm font-medium">{r.requirement}</p>
                        <p className="text-xs mt-1" style={{ color: 'var(--text-light)' }}>
                          {r.suggestedRevision?.substring(0, 100)}...
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {diff.length > 0 && (
            <div className="paper-card p-5">
              <h3 className="font-bold text-sm mb-3">🔍 修改对比</h3>
              <div className="space-y-2 text-sm">
                {diff.map((d, i) => (
                  <div key={i} className={`p-2 rounded-lg ${d.type === "ADDED" ? "bg-green-50 text-green-800" : "bg-red-50 text-red-800"}`}>
                    <span className="text-xs font-bold mr-2">{d.type === "ADDED" ? "+" : "-"}</span>
                    {d.content}
                  </div>
                ))}
              </div>
            </div>
          )}

          {!loading && !results && (
            <div className="paper-card p-8 text-center">
              <div className="text-4xl mb-3">📄</div>
              <p style={{ color: 'var(--text-light)' }}>配置返修参数后点击"开始返修"</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
