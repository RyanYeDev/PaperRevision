package com.paperrevision.domain.evaluation.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.paperrevision.domain.evaluation.model.AgentExecutionTraceEntity;
import com.paperrevision.domain.evaluation.model.EvaluationEntity;
import com.paperrevision.domain.evaluation.model.EvaluationRubric;
import com.paperrevision.domain.evaluation.model.LLMJudgeRequest;
import com.paperrevision.domain.evaluation.model.LLMJudgeResult;
import com.paperrevision.domain.evaluation.model.TrajectoryEvalResult;
import com.paperrevision.domain.evaluation.repository.AgentExecutionTraceRepository;
import com.paperrevision.domain.evaluation.repository.EvaluationRepository;

/**
 * 评估领域服务 - 编排评估流程
 *
 * 评估流程（Phase 1 + Phase 2）：
 * 1. 获取执行轨迹 → TrajectoryAnalyzer 计算 8 项轨迹指标
 * 2. LLM Judge 进行语义评估（5 维度评分 + 幻觉检测）
 * 3. 轨迹分和 LLM 裁判分加权融合 → 4 维度最终评分
 * 4. 持久化到 evaluations 表
 * 5. LLM 不可用时自动降级到纯轨迹评估
 *
 * 对应 Agent 评估知识体系：
 * - 结果评估 + 轨迹评估 双维度
 * - LLM 裁判 (Judge LLM)
 * - 幻觉率检测
 * - 降级策略
 */
@Service
public class EvaluationDomainService {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationDomainService.class);

    /** LLM Judge 在各维度评分中的权重 */
    private static final double LLM_WEIGHT = 0.65;
    /** 轨迹分析在各维度评分中的权重 */
    private static final double TRAJECTORY_WEIGHT = 0.35;

    private final EvaluationRepository evaluationRepository;
    private final TrajectoryAnalyzer trajectoryAnalyzer;
    private final AgentExecutionTraceRepository traceRepository;
    private final LLMJudgeService llmJudgeService;

    public EvaluationDomainService(EvaluationRepository evaluationRepository,
            TrajectoryAnalyzer trajectoryAnalyzer,
            AgentExecutionTraceRepository traceRepository,
            LLMJudgeService llmJudgeService) {
        this.evaluationRepository = evaluationRepository;
        this.trajectoryAnalyzer = trajectoryAnalyzer;
        this.traceRepository = traceRepository;
        this.llmJudgeService = llmJudgeService;
    }

    /**
     * 综合评估返修结果（轨迹分析 + LLM 裁判加权融合）
     *
     * @param revisionResultId 返修结果ID
     * @param userId           用户ID
     * @param revisionResult   返修结果数据
     * @param sessionId        执行追踪的 session ID
     * @return 持久化后的评估实体
     */
    public EvaluationEntity evaluateRevision(String revisionResultId, String userId,
            Map<String, Object> revisionResult, String sessionId) {

        // 1. 获取执行轨迹
        List<AgentExecutionTraceEntity> traces = traceRepository.findBySessionId(sessionId);

        // 2. 轨迹分析
        TrajectoryEvalResult trajResult = trajectoryAnalyzer.analyze(traces);

        // 3. LLM Judge 语义评估（带降级）
        LLMJudgeResult judgeResult = null;
        String evaluatorType = "TRAJECTORY";
        try {
            judgeResult = evaluateWithLLMJudge(revisionResult);
            evaluatorType = "LLM_JUDGE";
        } catch (Exception e) {
            logger.warn("LLM Judge 评估失败，降级到纯轨迹评估: {}", e.getMessage());
        }

        // 4. 加权融合：轨迹分 + LLM 裁判分
        double relevance;
        double faithfulness;
        double completeness;
        double formatScore;

        if (judgeResult != null) {
            // LLM Judge 主导，轨迹分析为辅
            relevance = judgeResult.getRelevanceScore() * LLM_WEIGHT
                    + trajResult.getToolAccuracy() * TRAJECTORY_WEIGHT;
            faithfulness = judgeResult.getFaithfulnessScore() * LLM_WEIGHT
                    + trajResult.getParamCorrectness() * TRAJECTORY_WEIGHT;
            completeness = judgeResult.getCompletenessScore() * LLM_WEIGHT
                    + trajResult.getStepEfficiency() * TRAJECTORY_WEIGHT;
            formatScore = judgeResult.getFormatScore() * LLM_WEIGHT
                    + computeFormatScore(revisionResult) * TRAJECTORY_WEIGHT;
        } else {
            // 纯轨迹评估（降级模式）
            relevance = trajResult.getToolAccuracy() * 0.6 + trajResult.getSuccessRate() * 0.4;
            faithfulness = trajResult.getParamCorrectness() * 0.7
                    + (1.0 - trajResult.getRedundancyRate()) * 0.3;
            completeness = trajResult.getStepEfficiency() * 0.6
                    + trajResult.getSuccessRate() * 0.4;
            formatScore = computeFormatScore(revisionResult) * 0.4
                    + trajResult.getParamCorrectness() * 0.6;
        }

        double overall = (relevance + faithfulness + completeness + formatScore) / 4.0;

        // 5. 生成反馈信息
        String feedback = buildFeedback(trajResult, judgeResult);

        // 6. 持久化
        EvaluationEntity eval = new EvaluationEntity();
        eval.setRevisionResultId(revisionResultId);
        eval.setRelevanceScore(relevance);
        eval.setFaithfulnessScore(faithfulness);
        eval.setCompletenessScore(completeness);
        eval.setFormatScore(formatScore);
        eval.setOverallScore(overall);
        eval.setFeedback(feedback);
        eval.setEvaluatorType(evaluatorType);
        eval.setUserId(userId);
        evaluationRepository.checkInsert(eval);

        logger.info("评估完成: evaluatorType={}, overallScore={}, grade={}",
                evaluatorType, String.format("%.3f", overall), eval.getGrade());

        return eval;
    }

    /**
     * 使用 LLM Judge 进行语义评估
     */
    private LLMJudgeResult evaluateWithLLMJudge(Map<String, Object> revisionResult) throws Exception {
        String originalText = extractString(revisionResult, "");
        String revisedText = extractString(revisionResult, "suggestedRevision");
        String requirement = extractString(revisionResult, "requirement");
        String referenceText = extractReferenceText(revisionResult);

        LLMJudgeRequest request = new LLMJudgeRequest(
                originalText, revisedText, requirement, referenceText,
                "论文返修助手", EvaluationRubric.REVISION_RUBRIC.toRubricPrompt());

        return llmJudgeService.evaluate(request);
    }

    /**
     * 查询某返修结果的所有评估历史
     */
    public List<EvaluationEntity> getEvaluationHistory(String revisionResultId) {
        LambdaQueryWrapper<EvaluationEntity> wrapper = Wrappers.<EvaluationEntity>lambdaQuery()
                .eq(EvaluationEntity::getRevisionResultId, revisionResultId)
                .orderByDesc(EvaluationEntity::getCreatedAt);
        return evaluationRepository.selectList(wrapper);
    }

    /** 从 revisionResult Map 中提取字符串字段 */
    private String extractString(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    /** 从 revisionResult 中提取参考文献文本 */
    private String extractReferenceText(Map<String, Object> revisionResult) {
        if (revisionResult == null) return "";
        Object refs = revisionResult.get("relevantReferences");
        if (refs instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Object ref : (List<?>) refs) {
                if (ref instanceof Map) {
                    Object content = ((Map<?, ?>) ref).get("content");
                    if (content != null) sb.append(content.toString()).append("\n---\n");
                }
            }
            return sb.toString();
        }
        return "";
    }

    /** 基础格式评分 */
    private double computeFormatScore(Map<String, Object> revisionResult) {
        if (revisionResult == null) return 0.5;

        String text = extractString(revisionResult, "suggestedRevision");
        if (text.isEmpty()) return 0.3;

        double score = 0.5;
        if (text.contains("\n")) score += 0.2;
        if (text.length() > 50) score += 0.2;
        if (text.contains("：") || text.contains(":") || text.contains("修改")) score += 0.1;

        return Math.min(1.0, score);
    }

    /** 生成人类可读的反馈信息（融合轨迹和 LLM Judge 信息） */
    private String buildFeedback(TrajectoryEvalResult traj, LLMJudgeResult judge) {
        StringBuilder sb = new StringBuilder();

        if (judge != null) {
            sb.append("【LLM 裁判评估】");
            if (judge.getReasoning() != null && !judge.getReasoning().isEmpty()) {
                sb.append(truncate(judge.getReasoning(), 500));
            }
            if (judge.getHallucinationScore() < 0.6) {
                sb.append("  ⚠️ 检测到可能的幻觉内容。");
            }
            sb.append(" | ");
        }

        sb.append("【轨迹分析】");
        sb.append(String.format("%d个步骤, %d成功", traj.getTotalSteps(), traj.getSuccessSteps()));

        if (traj.getStepEfficiency() < 0.5) {
            sb.append("；步骤效率偏低");
        }
        if (traj.getRedundancyRate() > 0.3) {
            sb.append(String.format("；冗余调用率 %.0f%%", traj.getRedundancyRate() * 100));
        }
        if (traj.getToolAccuracy() < 0.5) {
            sb.append("；工具准确性有待提升");
        }

        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
