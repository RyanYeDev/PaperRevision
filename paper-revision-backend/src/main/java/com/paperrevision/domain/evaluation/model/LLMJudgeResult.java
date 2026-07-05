package com.paperrevision.domain.evaluation.model;

/**
 * LLM 裁判结果 - 值对象
 * 包含 5 个维度的语义评分和详细推理过程
 */
public class LLMJudgeResult {

    /** 相关性：修改是否针对审稿意见的核心问题 (0.0-1.0) */
    private final double relevanceScore;

    /** 忠实度：修改是否忠实于参考文献，无捏造 (0.0-1.0) */
    private final double faithfulnessScore;

    /** 完整性：修改是否全面覆盖审稿意见的所有子要求 (0.0-1.0) */
    private final double completenessScore;

    /** 格式分：格式是否符合学术规范 (0.0-1.0) */
    private final double formatScore;

    /** 幻觉分：1.0=无幻觉, 0.0=严重幻觉 */
    private final double hallucinationScore;

    /** LLM 的详细推理说明 */
    private final String reasoning;

    /** LLM 原始响应（用于审计和调试） */
    private final String rawResponse;

    public LLMJudgeResult(double relevanceScore, double faithfulnessScore,
            double completenessScore, double formatScore,
            double hallucinationScore, String reasoning, String rawResponse) {
        this.relevanceScore = relevanceScore;
        this.faithfulnessScore = faithfulnessScore;
        this.completenessScore = completenessScore;
        this.formatScore = formatScore;
        this.hallucinationScore = hallucinationScore;
        this.reasoning = reasoning;
        this.rawResponse = rawResponse;
    }

    public double getRelevanceScore() { return relevanceScore; }
    public double getFaithfulnessScore() { return faithfulnessScore; }
    public double getCompletenessScore() { return completenessScore; }
    public double getFormatScore() { return formatScore; }
    public double getHallucinationScore() { return hallucinationScore; }
    public String getReasoning() { return reasoning; }
    public String getRawResponse() { return rawResponse; }
}
