package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.model.DocumentChunkEntity;
import org.xhy.domain.rag.repository.DocumentChunkRepository;
import org.xhy.infrastructure.rag.service.EmbeddingService;
import org.xhy.infrastructure.rag.service.MilvusVectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** RAG领域服务 */
@Service
public class RagDomainService {

    private static final Logger logger = LoggerFactory.getLogger(RagDomainService.class);

    private final DocumentChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final MilvusVectorStore vectorStore;
    private final DocumentChunkRepository chunkRepository;

    public RagDomainService(DocumentChunkingService chunkingService, EmbeddingService embeddingService,
            MilvusVectorStore vectorStore, DocumentChunkRepository chunkRepository) {
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.chunkRepository = chunkRepository;
    }

    /** 文档入库流程: 分块 -> 向量化 -> 存储 */
    public List<DocumentChunkEntity> indexDocument(String paperId, String text) {
        logger.info("开始文档索引: paperId={}, textLength={}", paperId, text.length());

        // 1. 分块
        List<String> chunks = chunkingService.chunk(text);
        if (chunks.isEmpty()) return new ArrayList<>();

        // 2. 向量化
        List<List<Float>> vectors = embeddingService.embedBatch(chunks);

        // 3. 存储到数据库
        List<DocumentChunkEntity> entities = new ArrayList<>();
        List<String> chunkIds = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = UUID.randomUUID().toString();
            chunkIds.add(chunkId);

            DocumentChunkEntity entity = new DocumentChunkEntity();
            entity.setPaperId(paperId);
            entity.setChunkIndex(i);
            entity.setContent(chunks.get(i));
            entity.setEmbeddingId(chunkId);
            chunkRepository.checkInsert(entity);
            entities.add(entity);
        }

        // 4. 存储到Milvus
        if (!vectors.isEmpty()) {
            vectorStore.insertVectors(paperId, chunkIds, chunks, vectors);
        }

        logger.info("文档索引完成: paperId={}, 分块数={}", paperId, chunks.size());
        return entities;
    }

    /** 获取论文的所有分块 */
    public List<DocumentChunkEntity> getPaperChunks(String paperId) {
        LambdaQueryWrapper<DocumentChunkEntity> wrapper = Wrappers.<DocumentChunkEntity>lambdaQuery()
                .eq(DocumentChunkEntity::getPaperId, paperId)
                .orderByAsc(DocumentChunkEntity::getChunkIndex);
        return chunkRepository.selectList(wrapper);
    }

    /** 删除论文的索引数据 */
    public void deletePaperIndex(String paperId) {
        LambdaQueryWrapper<DocumentChunkEntity> wrapper = Wrappers.<DocumentChunkEntity>lambdaQuery()
                .eq(DocumentChunkEntity::getPaperId, paperId);
        chunkRepository.checkedDelete(wrapper);
        logger.info("论文索引数据已删除: paperId={}", paperId);
    }
}
