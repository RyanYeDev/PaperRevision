package com.paperrevision.domain.evaluation.model;

/**
 * 轨迹评估结果 - 值对象
 * 包含 Agent 执行轨迹的多维度指标分析结果
 */
public class TrajectoryEvalResult {

    /** 步骤效率：预期步骤数 / 实际步骤数，越接近1越好 */
    private final double stepEfficiency;

    /** 工具选择准确率：选择正确工具的步骤占比 */
    private final double toolAccuracy;

    /** 参数正确率：工具参数格式/取值正确的调用占比 */
    private final double paramCorrectness;

    /** 冗余调用率：无效/重复调用占总调用的比例，越低越好 */
    private final double redundancyRate;

    /** 端到端延迟（毫秒）：所有步骤耗时总和 */
    private final long totalDurationMs;

    /** Token 总消耗 */
    private final int totalTokens;

    /** 执行成功率：状态为 SUCCESS 的步骤占比 */
    private final double successRate;

    /** 模型调用总次数 */
    private final int totalModelCalls;

    /** 总步骤数 */
    private final int totalSteps;

    /** 成功步骤数 */
    private final long successSteps;

    /** 有效工具调用次数（去重后的） */
    private final int effectiveToolCalls;

    /** 总工具调用次数 */
    private final int totalToolCalls;

    public TrajectoryEvalResult(double stepEfficiency, double toolAccuracy,
            double paramCorrectness, double redundancyRate,
            long totalDurationMs, int totalTokens, double successRate,
            int totalModelCalls, int totalSteps, long successSteps,
            int effectiveToolCalls, int totalToolCalls) {
        this.stepEfficiency = stepEfficiency;
        this.toolAccuracy = toolAccuracy;
        this.paramCorrectness = paramCorrectness;
        this.redundancyRate = redundancyRate;
        this.totalDurationMs = totalDurationMs;
        this.totalTokens = totalTokens;
        this.successRate = successRate;
        this.totalModelCalls = totalModelCalls;
        this.totalSteps = totalSteps;
        this.successSteps = successSteps;
        this.effectiveToolCalls = effectiveToolCalls;
        this.totalToolCalls = totalToolCalls;
    }

    public double getStepEfficiency() { return stepEfficiency; }
    public double getToolAccuracy() { return toolAccuracy; }
    public double getParamCorrectness() { return paramCorrectness; }
    public double getRedundancyRate() { return redundancyRate; }
    public long getTotalDurationMs() { return totalDurationMs; }
    public int getTotalTokens() { return totalTokens; }
    public double getSuccessRate() { return successRate; }
    public int getTotalModelCalls() { return totalModelCalls; }
    public int getTotalSteps() { return totalSteps; }
    public long getSuccessSteps() { return successSteps; }
    public int getEffectiveToolCalls() { return effectiveToolCalls; }
    public int getTotalToolCalls() { return totalToolCalls; }
}
