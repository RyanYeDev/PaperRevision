package com.paperrevision.infrastructure.llm;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * LLM调用服务
 * 通过 langchain4j 的 OpenAI-compatible 客户端对接 DeepSeek 和 Doubao
 *
 * DeepSeek API:  base_url="https://api.deepseek.com"
 * Doubao(豆包):   base_url="https://ark.cn-beijing.volces.com/api/v3"
 *
 * 两者都兼容 OpenAI chat/completions 接口格式
 */
@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);

    /**
     * 创建聊天模型客户端
     *
     * @param baseUrl API地址 (DeepSeek: https://api.deepseek.com, Doubao: https://ark.cn-beijing.volces.com/api/v3)
     * @param apiKey  API密钥
     * @param model  模型名称 (deepseek-chat, doubao-pro-32k 等)
     */
    public OpenAiChatModel createChatModel(String baseUrl, String apiKey, String model) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(2)
                .temperature(0.7)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /** 创建流式聊天模型 */
    public OpenAiStreamingChatModel createStreamingModel(String baseUrl, String apiKey, String model) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(120))
                .temperature(0.7)
                .build();
    }

    /** 测试连接 */
    public Map<String, Object> testConnection(String baseUrl, String apiKey, String model) {
        try {
            long start = System.currentTimeMillis();
            OpenAiChatModel chatModel = createChatModel(baseUrl, apiKey, model);
            String response = chatModel.generate("Hello, reply with just 'OK'.");
            long elapsed = System.currentTimeMillis() - start;

            logger.info("LLM连接测试成功: {} -> {}ms", model, elapsed);
            return Map.of(
                    "success", true,
                    "model", model,
                    "responseTime", elapsed + "ms",
                    "reply", response.trim()
            );
        } catch (Exception e) {
            logger.warn("LLM连接测试失败: {} - {}", model, e.getMessage());
            return Map.of(
                    "success", false,
                    "model", model,
                    "error", e.getMessage()
            );
        }
    }
}
