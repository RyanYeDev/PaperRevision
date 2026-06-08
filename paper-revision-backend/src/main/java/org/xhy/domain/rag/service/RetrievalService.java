package org.xhy.domain.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.model.DocumentChunkEntity;
import org.xhy.infrastructure.rag.service.EmbeddingService;
import org.xhy.infrastructure.rag.service.MilvusVectorStore;

import java.util.List;
import java.util.stream.Collectors;

/** RAG检索服务 - 向量检索 + 混合检索 */
@Service
public class RetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(RetrievalService.class);

    private final MilvusVectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final RagDomainService ragDomainService;

    public RetrievalService(MilvusVectorStore vectorStore, EmbeddingService embeddingService,
            RagDomainService ragDomainService) {
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
        this.ragDomainService = ragDomainService;
    }

    /** 向量检索 - 根据查询文本检索最相关的文档块 */
    public List<DocumentChunkEntity> vectorSearch(String query, String paperId, int topK) {
        logger.info("向量检索: query={}, paperId={}, topK={}", query, paperId, topK);

        // 1. 将查询向量化
        List<Float> queryVector = embeddingService.embed(query);

        // 2. Milvus检索
        List<String> chunkTexts = vectorStore.search(queryVector, topK);

        // 3. 根据检索结果获取完整的分块信息
        List<DocumentChunkEntity> allChunks = ragDomainService.getPaperChunks(paperId);

        return allChunks.stream()
                .filter(chunk -> chunkTexts.contains(chunk.getContent()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /** 关键词检索（简单BM25风格的文本匹配） */
    public List<DocumentChunkEntity> keywordSearch(String query, String paperId, int topK) {
        List<DocumentChunkEntity> chunks = ragDomainService.getPaperChunks(paperId);

        String[] keywords = query.toLowerCase().split("\\s+");

        return chunks.stream()
                .sorted((a, b) -> {
                    int scoreA = countKeywordHits(a.getContent().toLowerCase(), keywords);
                    int scoreB = countKeywordHits(b.getContent().toLowerCase(), keywords);
                    return Integer.compare(scoreB, scoreA);
                })
                .limit(topK)
                .collect(Collectors.toList());
    }

    private int countKeywordHits(String text, String[] keywords) {
        int count = 0;
        for (String keyword : keywords) {
            int idx = 0;
            while ((idx = text.indexOf(keyword, idx)) != -1) {
                count++;
                idx += keyword.length();
            }
        }
        return count;
    }
}
