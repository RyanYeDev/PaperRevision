package org.xhy.application.rag.service;

import org.springframework.stereotype.Service;
import org.xhy.domain.rag.model.DocumentChunkEntity;
import org.xhy.domain.rag.service.RagDomainService;
import org.xhy.domain.rag.service.RetrievalService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** RAG应用服务 */
@Service
public class RagAppService {

    private final RagDomainService ragDomainService;
    private final RetrievalService retrievalService;

    public RagAppService(RagDomainService ragDomainService, RetrievalService retrievalService) {
        this.ragDomainService = ragDomainService;
        this.retrievalService = retrievalService;
    }

    /** 为论文建立索引 */
    public int indexPaper(String paperId, String text) {
        List<DocumentChunkEntity> chunks = ragDomainService.indexDocument(paperId, text);
        return chunks.size();
    }

    /** RAG检索 */
    public Map<String, Object> search(String query, String paperId, int topK) {
        Map<String, Object> result = new HashMap<>();

        // 向量检索
        List<DocumentChunkEntity> vectorResults = retrievalService.vectorSearch(query, paperId, topK);
        // 关键词检索
        List<DocumentChunkEntity> keywordResults = retrievalService.keywordSearch(query, paperId, topK);

        result.put("vectorResults", vectorResults.stream()
                .map(c -> Map.of("content", c.getContent(), "chunkIndex", c.getChunkIndex()))
                .collect(Collectors.toList()));
        result.put("keywordResults", keywordResults.stream()
                .map(c -> Map.of("content", c.getContent(), "chunkIndex", c.getChunkIndex()))
                .collect(Collectors.toList()));

        return result;
    }
}
