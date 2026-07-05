package com.paperrevision.domain.evaluation.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.paperrevision.domain.evaluation.model.AgentExecutionTraceEntity;
import com.paperrevision.domain.evaluation.model.TrajectoryEvalResult;

/**
 * 轨迹分析器 - 纯领域服务
 * 从 Agent 执行轨迹中计算多维度评估指标，不依赖任何基础设施层
 *
 * 评估维度（对应 Agent 评估知识体系）：
 * - 任务成功率 (Task Success Rate)
 * - 步骤效率 (Step Efficiency)
 * - 工具选择准确率 (Tool Selection Accuracy)
 * - 参数正确率 (Parameter Correctness)
 * - 冗余调用率 (Redundant Call Rate)
 * - 端到端延迟 (E2E Latency)
 * - Token消耗 (Token Consumption)
 * - 模型调用次数 (Model Call Count)
 */
@Service
public class TrajectoryAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(TrajectoryAnalyzer.class);

    /** 论文返修工作流的预期核心步骤数 */
    private static final int EXPECTED_CORE_STEPS = 6;

    /**
     * 分析执行轨迹，产出多维度评估结果
     *
     * @param traces 按时间排序的执行轨迹列表
     * @return 包含 8 个维度指标的轨迹评估结果
     */
    public TrajectoryEvalResult analyze(List<AgentExecutionTraceEntity> traces) {
        if (traces == null || traces.isEmpty()) {
            logger.warn("轨迹为空，返回默认结果");
            return new TrajectoryEvalResult(0.0, 0.0, 0.0, 1.0,
                    0L, 0, 0.0, 0, 0, 0, 0, 0);
        }

        int totalSteps = traces.size();

        // 1. 步骤效率：预期步骤数 / 实际步骤数（clamp to [0,1]）
        double stepEfficiency = totalSteps > 0
                ? Math.min(1.0, (double) EXPECTED_CORE_STEPS / totalSteps) : 0.0;

        // 2. 工具选择准确率：检查每步的 stepType 是否为已知工具类型
        double toolAccuracy = computeToolAccuracy(traces);

        // 3. 参数正确率：目前基于步骤状态推断（无独立参数校验器时用成功率近似）
        double paramCorrectness = computeParamCorrectness(traces);

        // 4. 冗余调用率：去重检测
        double redundancyRate = computeRedundancyRate(traces);

        // 5. 端到端延迟
        long totalDurationMs = traces.stream()
                .filter(t -> t.getDurationMs() != null)
                .mapToLong(AgentExecutionTraceEntity::getDurationMs)
                .sum();

        // 6. Token 总消耗
        int totalTokens = traces.stream()
                .filter(t -> t.getTokensUsed() != null)
                .mapToInt(AgentExecutionTraceEntity::getTokensUsed)
                .sum();

        // 7. 执行成功率
        long successSteps = traces.stream()
                .filter(t -> "SUCCESS".equals(t.getStatus()))
                .count();
        double successRate = totalSteps > 0 ? (double) successSteps / totalSteps : 0.0;

        // 8. 模型调用总次数
        int totalModelCalls = traces.stream()
                .filter(t -> t.getModelCalls() != null)
                .mapToInt(AgentExecutionTraceEntity::getModelCalls)
                .sum();

        // 工具调用统计
        int totalToolCalls = traces.stream()
                .filter(t -> t.getToolCalls() != null)
                .mapToInt(AgentExecutionTraceEntity::getToolCalls)
                .sum();
        int effectiveToolCalls = computeEffectiveToolCalls(traces);

        logger.info("轨迹分析完成: {}步, 成功率={}, 步骤效率={}, 延迟={}ms, Token={}",
                totalSteps, String.format("%.2f", successRate),
                String.format("%.2f", stepEfficiency), totalDurationMs, totalTokens);

        return new TrajectoryEvalResult(
                stepEfficiency, toolAccuracy, paramCorrectness, redundancyRate,
                totalDurationMs, totalTokens, successRate, totalModelCalls,
                totalSteps, successSteps, effectiveToolCalls, totalToolCalls);
    }

    /**
     * 计算工具选择准确率
     * 通过检查 stepType 是否为已知工具名称来判断工具选择是否正确
     */
    private double computeToolAccuracy(List<AgentExecutionTraceEntity> traces) {
        List<AgentExecutionTraceEntity> toolSteps = traces.stream()
                .filter(t -> t.getStepType() != null && !t.getStepType().isEmpty())
                .toList();

        if (toolSteps.isEmpty()) return 0.5; // 无工具调用时给中性分

        int correctCount = 0;
        for (AgentExecutionTraceEntity step : toolSteps) {
            String stepType = step.getStepType();
            // 排除元步骤（START/END），这些不计入工具准确性统计
            if ("START".equalsIgnoreCase(stepType) || "END".equalsIgnoreCase(stepType)) {
                correctCount++;
                continue;
            }
            // 检查是否为已知工具类型
            if (KNOWN_TOOL_TYPES.contains(stepType)) {
                correctCount++;
            }
        }

        return (double) correctCount / toolSteps.size();
    }

    /**
     * 计算参数正确率
     * 当前基于步骤状态推断：SUCCESS 状态表示参数基本正确
     * FAILURE 且非工具故障 → 参数可能有问题
     */
    private double computeParamCorrectness(List<AgentExecutionTraceEntity> traces) {
        List<AgentExecutionTraceEntity> toolSteps = traces.stream()
                .filter(t -> t.getStepType() != null && !t.getStepType().isEmpty()
                        && !"START".equalsIgnoreCase(t.getStepType())
                        && !"END".equalsIgnoreCase(t.getStepType()))
                .toList();

        if (toolSteps.isEmpty()) return 0.5;

        long paramOk = toolSteps.stream()
                .filter(t -> "SUCCESS".equals(t.getStatus()))
                .count();

        return (double) paramOk / toolSteps.size();
    }

    /**
     * 计算冗余调用率
     * 检测同一 phase + stepType 是否被重复调用
     */
    private double computeRedundancyRate(List<AgentExecutionTraceEntity> traces) {
        List<AgentExecutionTraceEntity> toolSteps = traces.stream()
                .filter(t -> t.getStepType() != null && !t.getStepType().isEmpty()
                        && !"START".equalsIgnoreCase(t.getStepType())
                        && !"END".equalsIgnoreCase(t.getStepType()))
                .toList();

        if (toolSteps.isEmpty()) return 0.0;

        // 通过 phase+stepType 去重来检测冗余
        Set<String> uniqueKeys = new HashSet<>();
        int duplicates = 0;
        for (AgentExecutionTraceEntity step : toolSteps) {
            String key = step.getPhase() + ":" + step.getStepType();
            if (!uniqueKeys.add(key)) {
                duplicates++;
            }
        }

        return (double) duplicates / toolSteps.size();
    }

    /**
     * 计算有效工具调用次数（去重后的）
     */
    private int computeEffectiveToolCalls(List<AgentExecutionTraceEntity> traces) {
        Set<String> uniqueKeys = new HashSet<>();
        for (AgentExecutionTraceEntity step : traces) {
            if (step.getStepType() != null && !step.getStepType().isEmpty()
                    && !"START".equalsIgnoreCase(step.getStepType())
                    && !"END".equalsIgnoreCase(step.getStepType())) {
                uniqueKeys.add(step.getPhase() + ":" + step.getStepType());
            }
        }
        return uniqueKeys.size();
    }

    /** 论文返修工作流中已知的工具类型集合 */
    private static final Set<String> KNOWN_TOOL_TYPES = Set.of(
            "revision_execution", "revision_complete",
            "read_pdf", "search_references", "search_paper",
            "compare_text", "write_revision", "check_citation",
            "format_check", "grammar_check",
            "parse", "rag", "analyze", "generate", "execute", "check"
    );
}
