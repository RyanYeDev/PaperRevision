package com.paperrevision.application.evaluation.dto;

/** 轨迹评估结果 DTO（对应 Agent 评估知识体系的 8 大指标） */
public class TrajectoryEvalResultDTO {

    /** 步骤效率 */
    private double stepEfficiency;
    /** 工具选择准确率 */
    private double toolAccuracy;
    /** 参数正确率 */
    private double paramCorrectness;
    /** 冗余调用率 */
    private double redundancyRate;
    /** 端到端延迟（毫秒） */
    private long totalDurationMs;
    /** Token 总消耗 */
    private int totalTokens;
    /** 执行成功率 */
    private double successRate;
    /** 模型调用总次数 */
    private int totalModelCalls;
    /** 总步骤数 */
    private int totalSteps;
    /** 成功步骤数 */
    private long successSteps;
    /** 有效工具调用次数 */
    private int effectiveToolCalls;
    /** 总工具调用次数 */
    private int totalToolCalls;

    public double getStepEfficiency() { return stepEfficiency; }
    public void setStepEfficiency(double stepEfficiency) { this.stepEfficiency = stepEfficiency; }

    public double getToolAccuracy() { return toolAccuracy; }
    public void setToolAccuracy(double toolAccuracy) { this.toolAccuracy = toolAccuracy; }

    public double getParamCorrectness() { return paramCorrectness; }
    public void setParamCorrectness(double paramCorrectness) { this.paramCorrectness = paramCorrectness; }

    public double getRedundancyRate() { return redundancyRate; }
    public void setRedundancyRate(double redundancyRate) { this.redundancyRate = redundancyRate; }

    public long getTotalDurationMs() { return totalDurationMs; }
    public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }

    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public int getTotalModelCalls() { return totalModelCalls; }
    public void setTotalModelCalls(int totalModelCalls) { this.totalModelCalls = totalModelCalls; }

    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }

    public long getSuccessSteps() { return successSteps; }
    public void setSuccessSteps(long successSteps) { this.successSteps = successSteps; }

    public int getEffectiveToolCalls() { return effectiveToolCalls; }
    public void setEffectiveToolCalls(int effectiveToolCalls) { this.effectiveToolCalls = effectiveToolCalls; }

    public int getTotalToolCalls() { return totalToolCalls; }
    public void setTotalToolCalls(int totalToolCalls) { this.totalToolCalls = totalToolCalls; }
}
