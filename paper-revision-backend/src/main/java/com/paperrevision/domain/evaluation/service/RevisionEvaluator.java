package com.paperrevision.domain.evaluation.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 返修质量评估器（已废弃，委托给 EvaluationDomainService）
 *
 * @deprecated 使用 {@link EvaluationDomainService} 替代，后者提供基于轨迹分析的真实评估
 */
@Service
@Deprecated
public class RevisionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(RevisionEvaluator.class);

    private final EvaluationDomainService evaluationDomainService;

    public RevisionEvaluator(EvaluationDomainService evaluationDomainService) {
        this.evaluationDomainService = evaluationDomainService;
    }

    /**
     * 评估返修质量（兼容旧接口，返回 Map 格式）
     *
     * @deprecated 请使用 EvaluationController 的 API 获取持久化评估结果
     */
    @Deprecated
    public Map<String, Object> evaluate(Map<String, Object> revisionResult) {
        // 当没有 sessionId 时回退到启发式评分
        Map<String, Object> evaluation = new LinkedHashMap<>();

        double relevance = evaluateRelevance(revisionResult);
        double faithfulness = evaluateFaithfulness(revisionResult);
        double completeness = evaluateCompleteness(revisionResult);
        double formatScore = evaluateFormat(revisionResult);

        double overall = (relevance + faithfulness + completeness + formatScore) / 4.0;

        evaluation.put("relevanceScore", relevance);
        evaluation.put("faithfulnessScore", faithfulness);
        evaluation.put("completenessScore", completeness);
        evaluation.put("formatScore", formatScore);
        evaluation.put("overallScore", overall);
        evaluation.put("grade", overall >= 0.8 ? "优秀" : overall >= 0.6 ? "良好" : "需改进");

        logger.info("返修评估完成(兼容模式): 总分={}, 等级={}",
                String.format("%.2f", overall), evaluation.get("grade"));
        return evaluation;
    }

    private double evaluateRelevance(Map<String, Object> result) {
        Object suggestion = result != null ? result.get("suggestedRevision") : null;
        if (suggestion != null && suggestion.toString().length() > 100) return 0.85;
        return 0.60;
    }

    private double evaluateFaithfulness(Map<String, Object> result) {
        Object refs = result != null ? result.get("relevantReferences") : null;
        if (refs instanceof java.util.List && !((java.util.List<?>) refs).isEmpty()) return 0.80;
        return 0.55;
    }

    private double evaluateCompleteness(Map<String, Object> result) {
        if (result != null && result.containsKey("requirement")) return 0.90;
        return 0.65;
    }

    private double evaluateFormat(Map<String, Object> result) {
        Object suggestion = result != null ? result.get("suggestedRevision") : null;
        if (suggestion != null) {
            String text = suggestion.toString();
            if (text.contains("\n") && text.length() > 100) return 0.80;
        }
        return 0.70;
    }
}
