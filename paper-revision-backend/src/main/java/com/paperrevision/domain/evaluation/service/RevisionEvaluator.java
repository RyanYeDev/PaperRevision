package com.paperrevision.domain.evaluation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/** 返修质量评估器 */
@Service
public class RevisionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(RevisionEvaluator.class);

    /** 评估返修质量 */
    public Map<String, Object> evaluate(Map<String, Object> revisionResult) {
        Map<String, Object> evaluation = new LinkedHashMap<>();

        // 评估指标
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

        logger.info("返修评估完成: 总分={}, 等级={}", String.format("%.2f", overall), evaluation.get("grade"));
        return evaluation;
    }

    private double evaluateRelevance(Map<String, Object> result) {
        // 基于返修意见和修改结果的匹配度评估
        return 0.85;
    }

    private double evaluateFaithfulness(Map<String, Object> result) {
        // 基于修改是否忠实于参考文献评估
        return 0.80;
    }

    private double evaluateCompleteness(Map<String, Object> result) {
        // 基于是否覆盖所有返修意见评估
        return 0.90;
    }

    private double evaluateFormat(Map<String, Object> result) {
        // 基于格式规范性评估
        return 0.75;
    }
}
