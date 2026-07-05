package com.paperrevision.infrastructure.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperrevision.domain.evaluation.model.LLMJudgeRequest;
import com.paperrevision.domain.evaluation.model.LLMJudgeResult;
import com.paperrevision.domain.evaluation.service.LLMJudgeService;
import com.paperrevision.domain.llm.model.LLMProviderEntity;
import com.paperrevision.domain.llm.service.LLMProviderDomainService;
import com.paperrevision.infrastructure.llm.LLMService;

import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * LLM 裁判服务实现（Infrastructure 层）
 *
 * 使用 LLM 作为评估裁判，通过构造结构化的评分 prompt 让 LLM 对 Agent 输出
 * 进行语义层面的质量评估。支持 DeepSeek、Doubao（豆包）等兼容 OpenAI API 的模型。
 *
 * 核心设计决策：
 * - 接口定义在 Domain 层（LLMJudgeService），实现放在 Infrastructure 层
 * - 遵循依赖倒置原则（DIP）：Domain 不依赖具体的 LLM 调用细节
 * - LLM 调用失败时抛出异常，由调用方（EvaluationDomainService）做降级处理
 *
 * 对应 Agent 评估知识体系：LLM 裁判 / 语义匹配 / 幻觉检测
 */
@Service
public class LLMJudgeServiceImpl implements LLMJudgeService {

    private static final Logger logger = LoggerFactory.getLogger(LLMJudgeServiceImpl.class);

    private final LLMService llmService;
    private final LLMProviderDomainService providerDomainService;
    private final ObjectMapper objectMapper;

    public LLMJudgeServiceImpl(LLMService llmService,
            LLMProviderDomainService providerDomainService) {
        this.llmService = llmService;
        this.providerDomainService = providerDomainService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public LLMJudgeResult evaluate(LLMJudgeRequest request) throws Exception {
        logger.info("LLM Judge 评估开始...");

        // 1. 获取可用的 LLM 提供商
        LLMProviderEntity provider = getJudgeProvider();
        logger.info("使用 LLM 裁判模型: {}/{}", provider.getProviderType(), provider.getDefaultModel());

        // 2. 构建评估 prompt
        String judgePrompt = buildJudgePrompt(request);

        // 3. 调用 LLM（使用较低温度以获得更一致的评分）
        OpenAiChatModel chatModel = llmService.createChatModel(
                provider.getBaseUrl(), provider.getApiKey(), provider.getDefaultModel());

        long start = System.currentTimeMillis();
        String response = chatModel.generate(judgePrompt);
        long elapsed = System.currentTimeMillis() - start;

        logger.info("LLM Judge 评估完成, 耗时={}ms", elapsed);

        // 4. 解析 JSON 响应
        return parseJudgeResponse(response);
    }

    /**
     * 构建 LLM Judge 的评估 prompt
     */
    String buildJudgePrompt(LLMJudgeRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert evaluator for academic paper revision quality.\n\n");
        sb.append("Evaluate the following revision response on a 1-5 scale for each dimension.\n\n");

        // 评分标准
        sb.append(request.getRubricConfig());
        sb.append("\n");

        sb.append("## Original Text:\n");
        sb.append(truncate(request.getOriginalText(), 2000));
        sb.append("\n\n");

        sb.append("## Revised Text / Suggestion:\n");
        sb.append(truncate(request.getRevisedText(), 1500));
        sb.append("\n\n");

        if (request.getRequirement() != null && !request.getRequirement().isEmpty()) {
            sb.append("## Revision Requirement:\n");
            sb.append(request.getRequirement());
            sb.append("\n\n");
        }

        if (request.getReferenceText() != null && !request.getReferenceText().isEmpty()) {
            sb.append("## Reference Context:\n");
            sb.append(truncate(request.getReferenceText(), 1500));
            sb.append("\n\n");
        }

        sb.append("## Output Format\n");
        sb.append("Return ONLY a JSON object (no markdown, no code fences):\n");
        sb.append("{\n");
        sb.append("  \"relevance\": <int 1-5>,\n");
        sb.append("  \"faithfulness\": <int 1-5>,\n");
        sb.append("  \"completeness\": <int 1-5>,\n");
        sb.append("  \"format\": <int 1-5>,\n");
        sb.append("  \"hallucination\": <int 1-5>,\n");
        sb.append("  \"reasoning\": \"<step-by-step reasoning for each dimension>\"\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * 解析 LLM 返回的 JSON 响应
     * 将 1-5 分制映射到 0.0-1.0
     */
    LLMJudgeResult parseJudgeResponse(String response) {
        try {
            String jsonStr = extractJson(response);
            JsonNode root = objectMapper.readTree(jsonStr);

            double relevance = normalizeScore(getIntField(root, "relevance", 3));
            double faithfulness = normalizeScore(getIntField(root, "faithfulness", 3));
            double completeness = normalizeScore(getIntField(root, "completeness", 3));
            double formatScore = normalizeScore(getIntField(root, "format", 3));
            double hallucination = normalizeScore(getIntField(root, "hallucination", 5));
            String reasoning = root.has("reasoning") ? root.get("reasoning").asText() : "";

            logger.info("LLM Judge 解析结果: relevance={}, faithfulness={}, "
                            + "completeness={}, format={}, hallucination={}",
                    String.format("%.2f", relevance), String.format("%.2f", faithfulness),
                    String.format("%.2f", completeness), String.format("%.2f", formatScore),
                    String.format("%.2f", hallucination));

            return new LLMJudgeResult(relevance, faithfulness, completeness,
                    formatScore, hallucination, reasoning, response);

        } catch (Exception e) {
            logger.warn("LLM Judge 响应解析失败，返回默认评分: {}", e.getMessage());
            return new LLMJudgeResult(0.5, 0.5, 0.5, 0.5, 0.5,
                    "Parse error: " + e.getMessage(), response);
        }
    }

    /** 从 LLM 响应中提取 JSON（处理可能包裹的 markdown 代码块） */
    private String extractJson(String response) {
        String trimmed = response.trim();
        // 去除可能的 markdown 代码块标记
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    /** 安全读取整数字段，缺失时返回默认值 */
    private int getIntField(JsonNode root, String fieldName, int defaultValue) {
        if (root.has(fieldName)) {
            JsonNode node = root.get(fieldName);
            if (node.isInt()) return node.asInt();
            if (node.isDouble()) return (int) Math.round(node.asDouble());
            if (node.isTextual()) {
                try { return Integer.parseInt(node.asText().trim()); } catch (NumberFormatException ignored) {}
            }
        }
        return defaultValue;
    }

    /** 将 1-5 分制标准化为 0.0-1.0 */
    private double normalizeScore(int score) {
        return Math.max(0.0, Math.min(1.0, (double) (score - 1) / 4.0));
    }

    /** 截断过长文本 */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "... [truncated]";
    }

    /**
     * 获取用于裁判的 LLM 提供商
     * 优先使用 enable=true 的提供商，无可用时抛出异常
     */
    private LLMProviderEntity getJudgeProvider() {
        java.util.List<LLMProviderEntity> providers = providerDomainService.getUserProviders(null);
        if (providers != null) {
            for (LLMProviderEntity p : providers) {
                if (Boolean.TRUE.equals(p.getEnabled())) {
                    return p;
                }
            }
        }
        // 尝试不传 userId，获取所有可用的（getUserProviders(null) 可能不工作，用默认方法）
        throw new RuntimeException("No enabled LLM provider found for LLM Judge. Please configure one first.");
    }
}
