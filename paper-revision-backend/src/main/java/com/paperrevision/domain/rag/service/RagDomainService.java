package com.paperrevision.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.paperrevision.domain.rag.model.DocumentChunkEntity;
import com.paperrevision.domain.rag.repository.DocumentChunkRepository;
import com.paperrevision.infrastructure.rag.service.EmbeddingService;
import com.paperrevision.infrastructure.rag.service.MilvusVectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** RAG领域服务 */
@Service
public class RagDomainService {

    private static final Logger logger = LoggerFactory.getLogger(RagDomainService.class);

    private final DocumentChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;

    @Autowired(required = false)
    private MilvusVectorStore vectorStore;

    public RagDomainService(DocumentChunkingService chunkingService, EmbeddingService embeddingService,
            DocumentChunkRepository chunkRepository) {
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
    }

    /** 文档入库流程: 分块 -> 存DB -> (可选)向量化存入Milvus */
    public List<DocumentChunkEntity> indexDocument(String paperId, String text) {
        logger.info("开始文档索引: paperId={}, textLength={}", paperId, text.length());

        List<String> chunks = chunkingService.chunk(text);
        if (chunks.isEmpty()) return new ArrayList<>();

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

        // 仅在Milvus可用时存储向量
        if (vectorStore != null) {
            List<List<Float>> vectors = embeddingService.embedBatch(chunks);
            if (!vectors.isEmpty()) {
                vectorStore.insertVectors(paperId, chunkIds, chunks, vectors);
            }
        } else {
            logger.info("Milvus未启用，跳过向量存储，仅使用数据库检索");
        }

        logger.info("文档索引完成: paperId={}, 分块数={}", paperId, chunks.size());
        return entities;
    }

    public List<DocumentChunkEntity> getPaperChunks(String paperId) {
        LambdaQueryWrapper<DocumentChunkEntity> wrapper = Wrappers.<DocumentChunkEntity>lambdaQuery()
                .eq(DocumentChunkEntity::getPaperId, paperId)
                .orderByAsc(DocumentChunkEntity::getChunkIndex);
        return chunkRepository.selectList(wrapper);
    }

    public void deletePaperIndex(String paperId) {
        LambdaQueryWrapper<DocumentChunkEntity> wrapper = Wrappers.<DocumentChunkEntity>lambdaQuery()
                .eq(DocumentChunkEntity::getPaperId, paperId);
        chunkRepository.checkedDelete(wrapper);
        logger.info("论文索引数据已删除: paperId={}", paperId);
    }
}
