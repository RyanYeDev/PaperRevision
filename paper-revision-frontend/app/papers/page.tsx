"use client";

import { useState, useEffect } from "react";
import { api } from "@/lib/api";
import Link from "next/link";

interface Paper {
  id: string; title: string; fileName: string; status: string; pageCount: number; createdAt: string;
}

export default function PapersPage() {
  const [papers, setPapers] = useState<Paper[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => { loadPapers(); }, []);

  async function loadPapers() {
    try {
      const res = await api.papers.list();
      setPapers((res as { data: { records: Paper[] } }).data?.records || []);
    } finally { setLoading(false); }
  }

  async function handleUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      await api.papers.upload(file);
      setMessage("");
      await loadPapers();
    } catch {
      setMessage("上传失败，请重试");
    } finally { setUploading(false); }
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-display font-bold" style={{ color: "var(--text)" }}>📄 论文管理</h2>
        <label className="btn-cartoon btn-primary px-5 py-2 rounded-full text-sm cursor-pointer">
          {uploading ? "⏳ 上传中..." : "上传论文 PDF"}
          <input type="file" accept=".pdf" onChange={handleUpload} className="hidden" disabled={uploading} />
        </label>
      </div>

      {message && (
        <div className="mb-4 p-3 rounded-soft text-sm border"
          style={{ background: "var(--danger-tint)", borderColor: "var(--danger)", color: "#b45454" }}>
          {message}
          <button className="ml-3 underline" onClick={() => setMessage("")}>关闭</button>
        </div>
      )}

      {loading ? (
        <div className="text-center py-12" style={{ color: "var(--text-light)" }}>加载中...</div>
      ) : papers.length === 0 ? (
        <div className="paper-card text-center py-14">
          <div className="text-4xl mb-3">📄</div>
          <p className="mb-4" style={{ color: "var(--text-light)" }}>还没有上传论文</p>
          <label className="btn-cartoon btn-primary px-5 py-2 rounded-full text-sm cursor-pointer inline-block">
            上传第一篇论文
            <input type="file" accept=".pdf" onChange={handleUpload} className="hidden" />
          </label>
        </div>
      ) : (
        <div className="grid gap-4">
          {papers.map((paper, i) => (
            <Link key={paper.id} href={`/revision?paperId=${paper.id}`}
              className={`paper-card reveal reveal-${(i % 6) + 1} block p-5`}>
              <div className="flex justify-between items-start">
                <div className="min-w-0">
                  <h3 className="font-display font-bold text-lg truncate" style={{ color: "var(--text)" }}>{paper.fileName}</h3>
                  <p className="text-sm" style={{ color: "var(--text-light)" }}>
                    {paper.pageCount}页 · {new Date(paper.createdAt).toLocaleDateString("zh-CN")}
                  </p>
                </div>
                <span className={`badge ${
                  paper.status === "PARSED" ? "badge-success" :
                  paper.status === "UPLOADED" ? "badge-warn" :
                  "badge-primary"}`}>
                  {paper.status}
                </span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
