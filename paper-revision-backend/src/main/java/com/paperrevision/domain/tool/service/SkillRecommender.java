package com.paperrevision.domain.tool.service;

import com.paperrevision.domain.tool.service.SkillRegistry.SkillDefinition;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Skill 推荐排序器 — Skill 自动进化 Step 3
 *
 * 基于使用统计数据（成功率先前数据来自 SkillRegistry 的 Step1 追踪字段）和
 * 上下文匹配度，对技能列表进行加权评分排序。冷启动时默认成功率 50%，使用越多越准确。
 *
 * 依赖：{@link SkillRegistry}（通过 Spring 构造注入；单测可由 setter 注入）
 */
@Service
public class SkillRecommender {

    private final SkillRegistry registry;

    /** 权重配置 */
    private static final double W_SUCCESS_RATE = 0.4;
    private static final double W_USAGE = 0.3;
    private static final double W_MATCH = 0.3;
    /** 冷启动默认成功率（50%） */
    private static final double COLD_START_RATE = 0.5;

    public SkillRecommender(SkillRegistry registry) {
        this.registry = registry;
    }

    /**
     * 基于成功率、使用频次、上下文关键词匹配度进行加权评分排序
     * @param contextKeywords 当前上下文的特征词汇，用于匹配技能相关性
     * @return 按评分降序排列的推荐结果（含分数），最多返回 topN 条
     */
    public List<Recommendation> recommend(List<String> contextKeywords, int topN) {
        final List<String> keywords = contextKeywords == null ? List.of() : contextKeywords;

        int maxUse = registry.getAllSkills().stream()
                .mapToInt(SkillDefinition::getUseCount).max().orElse(1);

        return registry.getAllSkills().stream()
                .map(s -> new Recommendation(s, score(s, keywords, maxUse)))
                .sorted(Comparator.comparingDouble(Recommendation::score).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    /** 加权评分 */
    double score(SkillDefinition skill, List<String> keywords, int maxUse) {
        double successScore = skill.getUseCount() > 0
                ? skill.getSuccessRate()
                : COLD_START_RATE;
        double usageScore = maxUse > 0 ? (double) skill.getUseCount() / maxUse : 0;
        double matchScore = keywords.isEmpty() ? COLD_START_RATE
                : keywords.stream().anyMatch(k ->
                    skill.getName().contains(k) ||
                    skill.getDescription().contains(k) ||
                    skill.getCapabilities().stream().anyMatch(c -> c.contains(k))
                ) ? 1.0 : 0.0;

        return W_SUCCESS_RATE * successScore + W_USAGE * usageScore + W_MATCH * matchScore;
    }

    /** 推荐结果 */
    public record Recommendation(SkillDefinition skill, double score) {}
}
