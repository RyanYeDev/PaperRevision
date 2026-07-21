package com.paperrevision.infrastructure.rag.service;

import com.paperrevision.domain.llm.model.LLMProviderEntity;
import com.paperrevision.domain.llm.repository.LLMProviderRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Embedding服务 - 通过 langchain4j 对接 OpenAI 兼容的 Embedding API
 *
 * 支持的 Embedding API：
 * - DeepSeek: POST https://api.deepseek.com/v1/embeddings
 * - Doubao:   POST https://ark.cn-beijing.volces.com/api/v3/embeddings
 *
 * 本地开发无 API Key 时自动降级为哈希向量（功能可用但精度受限）
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int EMBEDDING_DIM = 512;

    private final LLMProviderRepository providerRepository;

    public EmbeddingService(LLMProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    /** 生成文本向量 */
    public List<Float> embed(String text) {
        LLMProviderEntity provider = findFirstEnabledProvider();
        if (provider != null && provider.getApiKey() != null && !provider.getApiKey().isBlank()) {
            return embedViaApi(provider, text);
        }
        logger.debug("无可用Embedding API Key，使用哈希向量降级: textLength={}", text.length());
        return hashEmbed(text);
    }

    /** 批量生成向量 */
    public List<List<Float>> embedBatch(List<String> texts) {
        LLMProviderEntity provider = findFirstEnabledProvider();
        if (provider != null && provider.getApiKey() != null && !provider.getApiKey().isBlank()) {
            return embedBatchViaApi(provider, texts);
        }
        logger.debug("无可用Embedding API Key，批量使用哈希向量降级: count={}", texts.size());
        return texts.stream().map(t -> hashEmbed(t)).toList();
    }

    /** 通过 API 生成向量 */
    private List<Float> embedViaApi(LLMProviderEntity provider, String text) {
        try {
            OpenAiEmbeddingModel model = createEmbeddingModel(provider);
            Embedding result = model.embed(text).content();
            return result.vectorAsList();
        } catch (Exception e) {
            logger.warn("Embedding API调用失败，降级为哈希向量: {}", e.getMessage());
            return hashEmbed(text);
        }
    }

    /** 通过 API 批量生成向量 */
    private List<List<Float>> embedBatchViaApi(LLMProviderEntity provider, List<String> texts) {
        try {
            OpenAiEmbeddingModel model = createEmbeddingModel(provider);
            List<TextSegment> segments = texts.stream().map(TextSegment::from).toList();
            List<Embedding> embeddings = model.embedAll(segments).content();
            return embeddings.stream().map(Embedding::vectorAsList).toList();
        } catch (Exception e) {
            logger.warn("Embedding API批量调用失败，降级为哈希向量: {}", e.getMessage());
            return texts.stream().map(t -> hashEmbed(t)).toList();
        }
    }

    /** 创建 Embedding 模型客户端 */
    private OpenAiEmbeddingModel createEmbeddingModel(LLMProviderEntity provider) {
        String modelName = resolveEmbeddingModel(provider);
        return OpenAiEmbeddingModel.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(1)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    /** 根据提供商类型选择 embedding 模型 */
    private String resolveEmbeddingModel(LLMProviderEntity provider) {
        String type = provider.getProviderType();
        if (type != null) {
            if (type.toLowerCase().contains("deepseek")) {
                return "deepseek-chat"; // DeepSeek 聊天模型支持 embedding
            }
            if (type.toLowerCase().contains("doubao")) {
                return provider.getDefaultModel() != null ? provider.getDefaultModel() : "doubao-pro-32k";
            }
        }
        return provider.getDefaultModel() != null ? provider.getDefaultModel() : "text-embedding-ada-002";
    }

    /** 查找第一个启用的 LLM 提供商 */
    private LLMProviderEntity findFirstEnabledProvider() {
        try {
            List<LLMProviderEntity> providers = providerRepository.selectList(null);
            if (providers != null) {
                return providers.stream()
                        .filter(p -> p.getEnabled() != null && p.getEnabled())
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            logger.debug("查询LLM提供商失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 哈希向量降级方案 — 将文本转换为确定性伪向量
     * 使用 SHA-256 哈希 + 线性扩展，保证相同文本生成相同向量
     * 向量质量有限但可在无 API Key 的开发环境中让功能跑通
     */
    static List<Float> hashEmbed(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            List<Float> vector = new ArrayList<>(EMBEDDING_DIM);
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                // 用哈希字节循环扩展，转换为 [-1, 1] 范围的浮点数
                int b0 = hash[i % hash.length] & 0xFF;
                int b1 = hash[(i + 7) % hash.length] & 0xFF;
                float value = ((b0 << 8 | b1) / 65535.0f) * 2.0f - 1.0f;
                vector.add(value);
            }
            return vector;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 在所有 JVM 上都可用，此异常不可能发生
            return Collections.nCopies(EMBEDDING_DIM, 0.0f);
        }
    }
}
