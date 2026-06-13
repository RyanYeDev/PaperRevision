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
      await loadPapers();
    } finally { setUploading(false); }
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold">论文管理</h2>
        <label className="px-4 py-2 bg-blue-600 text-white rounded-lg cursor-pointer hover:bg-blue-700">
          {uploading ? "上传中..." : "上传论文PDF"}
          <input type="file" accept=".pdf" onChange={handleUpload} className="hidden" disabled={uploading} />
        </label>
      </div>

      {loading ? (
        <div className="text-center py-12 text-gray-500">加载中...</div>
      ) : papers.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-lg border">
          <p className="text-gray-500 mb-4">还没有上传论文</p>
          <label className="px-4 py-2 bg-blue-600 text-white rounded-lg cursor-pointer inline-block">
            上传第一篇论文
            <input type="file" accept=".pdf" onChange={handleUpload} className="hidden" />
          </label>
        </div>
      ) : (
        <div className="grid gap-4">
          {papers.map((paper) => (
            <Link key={paper.id} href={`/revision?paperId=${paper.id}`}
              className="block p-4 bg-white rounded-lg border hover:shadow-md transition-shadow">
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="font-semibold text-lg">{paper.fileName}</h3>
                  <p className="text-sm text-gray-500">
                    {paper.pageCount}页 · {new Date(paper.createdAt).toLocaleDateString("zh-CN")}
                  </p>
                </div>
                <span className={`px-2 py-1 text-xs rounded-full ${
                  paper.status === "PARSED" ? "bg-green-100 text-green-700" :
                  paper.status === "UPLOADED" ? "bg-yellow-100 text-yellow-700" :
                  "bg-gray-100 text-gray-700"}`}>
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
