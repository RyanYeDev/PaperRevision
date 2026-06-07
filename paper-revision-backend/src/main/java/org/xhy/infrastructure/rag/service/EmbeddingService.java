package org.xhy.infrastructure.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Embedding服务 - 调用LLM生成向量
 * 实际部署时对接DeepSeek/Doubao的Embedding API
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    /** 生成文本向量（待接入实际Embedding API后替换为HTTP调用） */
    public List<Float> embed(String text) {
        // TODO: 对接实际Embedding API
        // DeepSeek Embedding: POST https://api.deepseek.com/v1/embeddings
        // Doubao Embedding: POST https://ark.cn-beijing.volces.com/api/v3/embeddings
        logger.debug("生成Embedding向量, 文本长度: {}", text.length());
        return Collections.emptyList();
    }

    /** 批量生成向量 */
    public List<List<Float>> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }
}
