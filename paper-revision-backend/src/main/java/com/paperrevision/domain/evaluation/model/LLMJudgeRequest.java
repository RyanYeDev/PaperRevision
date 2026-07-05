package com.paperrevision.domain.evaluation.model;

/**
 * LLM 裁判请求 - 值对象
 * 封装送给 LLM Judge 进行评估所需的所有上下文信息
 */
public class LLMJudgeRequest {

    private final String originalText;
    private final String revisedText;
    private final String requirement;
    private final String referenceText;
    private final String systemPrompt;
    private final String rubricConfig;

    public LLMJudgeRequest(String originalText, String revisedText,
            String requirement, String referenceText,
            String systemPrompt, String rubricConfig) {
        this.originalText = originalText;
        this.revisedText = revisedText;
        this.requirement = requirement;
        this.referenceText = referenceText;
        this.systemPrompt = systemPrompt;
        this.rubricConfig = rubricConfig;
    }

    public String getOriginalText() { return originalText; }
    public String getRevisedText() { return revisedText; }
    public String getRequirement() { return requirement; }
    public String getReferenceText() { return referenceText; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getRubricConfig() { return rubricConfig; }
}
