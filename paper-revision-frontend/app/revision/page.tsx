"use client";

import { useState } from "react";
import { api } from "@/lib/api";

export default function RevisionPage() {
  const [paperId, setPaperId] = useState("");
  const [refPaperId, setRefPaperId] = useState("");
  const [comments, setComments] = useState("");
  const [results, setResults] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  async function handleExecute() {
    if (!paperId || !comments.trim()) return;
    setLoading(true);
    try {
      const revisionComments = comments.split("\n").filter((c) => c.trim());
      const res = await api.revision.execute({ paperId, referencePaperId: refPaperId, revisionComments });
      setResults(res);
    } finally { setLoading(false); }
  }

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">返修中心</h2>

      <div className="grid grid-cols-2 gap-6">
        <div className="space-y-4">
          <div className="p-4 bg-white rounded-lg border">
            <h3 className="font-semibold mb-3">返修配置</h3>
            <div className="space-y-3">
              <div>
                <label className="block text-sm text-gray-600 mb-1">论文ID</label>
                <input type="text" value={paperId} onChange={(e) => setPaperId(e.target.value)}
                  className="w-full border rounded px-3 py-2 text-sm" placeholder="从论文管理页面获取" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">参考文献ID (可选)</label>
                <input type="text" value={refPaperId} onChange={(e) => setRefPaperId(e.target.value)}
                  className="w-full border rounded px-3 py-2 text-sm" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">返修意见 (每行一条)</label>
                <textarea rows={6} value={comments} onChange={(e) => setComments(e.target.value)}
                  className="w-full border rounded px-3 py-2 text-sm"
                  placeholder={"1. Reviewer建议补充实验对比...\n2. 引言部分需要增加文献综述...\n3. 格式需符合会议模板要求..."} />
              </div>
              <button onClick={handleExecute} disabled={loading || !paperId || !comments.trim()}
                className="w-full py-2 bg-blue-600 text-white rounded-lg disabled:opacity-50 hover:bg-blue-700">
                {loading ? "执行中..." : "开始返修"}
              </button>
            </div>
          </div>
        </div>

        <div>
          <div className="p-4 bg-white rounded-lg border min-h-[400px]">
            <h3 className="font-semibold mb-3">返修结果</h3>
            {results ? (
              <pre className="text-sm text-gray-700 overflow-auto max-h-[500px] bg-gray-50 p-3 rounded">
                {JSON.stringify(results, null, 2)}
              </pre>
            ) : (
              <p className="text-sm text-gray-400">配置返修参数并点击"开始返修"查看结果</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
