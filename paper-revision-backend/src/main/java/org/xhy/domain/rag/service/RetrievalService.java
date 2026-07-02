package org.xhy.domain.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final EmbeddingService embeddingService;
    private final RagDomainService ragDomainService;

    @Autowired(required = false)
    private MilvusVectorStore vectorStore;

    public RetrievalService(EmbeddingService embeddingService, RagDomainService ragDomainService) {
        this.embeddingService = embeddingService;
        this.ragDomainService = ragDomainService;
    }

    /** 向量检索（Milvus可用时）或关键词检索（fallback） */
    public List<DocumentChunkEntity> vectorSearch(String query, String paperId, int topK) {
        if (vectorStore != null) {
            logger.info("向量检索: query={}, paperId={}, topK={}", query, paperId, topK);
            List<Float> queryVector = embeddingService.embed(query);
            List<String> chunkTexts = vectorStore.search(queryVector, topK);
            List<DocumentChunkEntity> allChunks = ragDomainService.getPaperChunks(paperId);
            return allChunks.stream()
                    .filter(chunk -> chunkTexts.contains(chunk.getContent()))
                    .limit(topK)
                    .collect(Collectors.toList());
        }
        // Milvus不可用时使用关键词检索作为fallback
        logger.info("Milvus不可用，使用关键词检索作为fallback");
        return keywordSearch(query, paperId, topK);
    }

    /** 关键词检索 */
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
