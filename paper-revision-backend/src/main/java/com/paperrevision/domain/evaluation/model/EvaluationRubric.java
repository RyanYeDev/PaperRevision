package com.paperrevision.domain.evaluation.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 评估评分标准 - 值对象
 * 定义各维度的评分描述（1-5 分制），供 LLM Judge 使用
 *
 * 对应 Agent 评估知识体系中的 "rubric scoring"
 */
public class EvaluationRubric {

    /** 评分维度：维度名 → 评分标准描述 */
    private final Map<String, String> scoringDimensions;

    /** 检测任务：检测类型 → 任务描述 */
    private final Map<String, String> detectionTasks;

    public EvaluationRubric(Map<String, String> scoringDimensions,
            Map<String, String> detectionTasks) {
        this.scoringDimensions = new LinkedHashMap<>(scoringDimensions);
        this.detectionTasks = new LinkedHashMap<>(detectionTasks);
    }

    public Map<String, String> getScoringDimensions() { return scoringDimensions; }
    public Map<String, String> getDetectionTasks() { return detectionTasks; }

    /** 构建 LLM Judge 的评分标准 prompt 文本 */
    public String toRubricPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Scoring Dimensions (1-5 scale, where 1=worst, 5=best):\n");
        for (Map.Entry<String, String> entry : scoringDimensions.entrySet()) {
            sb.append(String.format("- **%s**: %s\n", entry.getKey(), entry.getValue()));
        }
        if (!detectionTasks.isEmpty()) {
            sb.append("\n## Detection Tasks:\n");
            for (Map.Entry<String, String> entry : detectionTasks.entrySet()) {
                sb.append(String.format("- **%s**: %s\n", entry.getKey(), entry.getValue()));
            }
        }
        return sb.toString();
    }

    // ==================== 预定义的默认评分标准 ====================

    /** 论文返修质量评估的默认评分标准 */
    public static final EvaluationRubric REVISION_RUBRIC = new EvaluationRubric(
            Map.of(
                    "relevance", "评价修改内容是否针对审稿意见的核心问题。"
                            + "1=完全不相关，偏离核心问题；5=完全命中核心问题，精准回应",
                    "faithfulness", "评价修改是否忠实于参考文献内容，没有捏造数据或结论。"
                            + "1=大量捏造，无法在参考文献中找到依据；5=完全基于原文，引用准确",
                    "completeness", "评价修改是否全面覆盖了审稿意见的所有子要求。"
                            + "1=完全遗漏多个关键要求；5=全面覆盖所有子要求，无遗漏",
                    "format", "评价格式是否符合学术规范（引用格式、术语统一、结构清晰）。"
                            + "1=格式混乱，不符合基本规范；5=完全符合学术写作规范"
            ),
            Map.of(
                    "hallucination_detection", "检查修改内容中是否存在："
                            + "(a) 参考文献中不存在的数据或结论；"
                            + "(b) 编造的引用来源；"
                            + "(c) 与参考文本矛盾的陈述。"
                            + "如发现幻觉，faithfulness 分应显著降低"
            )
    );
}
