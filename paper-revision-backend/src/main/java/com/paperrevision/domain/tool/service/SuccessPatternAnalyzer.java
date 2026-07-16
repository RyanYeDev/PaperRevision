package com.paperrevision.domain.tool.service;

import com.paperrevision.domain.tool.model.SkillUsageEntity;
import com.paperrevision.domain.tool.repository.SkillUsageRepository;
import com.paperrevision.domain.tool.service.SkillRegistry.SkillDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 成功模式分析器 — Skill 自动进化 Step 4
 *
 * 分析 skill_usage_log 中高成功率 case 的共同特征，提取高频 capability 词汇，
 * 聚类相似 revision 场景，为 Step 5(自动生成建议) 提供数据支撑。
 *
 * <p>依赖：{@link SkillRegistry}(技能元数据) + {@link SkillUsageRepository}(使用日志)
 * —— 仓储为可选注入，无数据时各方法返回空列表不抛异常。
 */
@Service
public class SuccessPatternAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(SuccessPatternAnalyzer.class);

    private final SkillRegistry registry;

    @Autowired(required = false)
    private SkillUsageRepository usageRepository;

    public SuccessPatternAnalyzer(SkillRegistry registry) {
        this.registry = registry;
    }

    /** 仅测试用 */
    void setUsageRepository(SkillUsageRepository repo) { this.usageRepository = repo; }

    // ---- 成功模式分析 ----

    /** 分析高成功率技能的能力词汇模式。
     * @param minRate 最低成功率阈值（如 0.8 = 80%），低于此的技能不参与分析 */
    public List<CapabilityPattern> analyzePatterns(double minRate) {
        List<SkillDefinition> highPerformers = registry.getAllSkills().stream()
                .filter(s -> s.getUseCount() > 0 && s.getSuccessRate() >= minRate)
                .collect(Collectors.toList());
        if (highPerformers.isEmpty()) return List.of();

        Map<String, Integer> capFreq = new HashMap<>();
        for (SkillDefinition skill : highPerformers) {
            for (String cap : skill.getCapabilities()) {
                capFreq.merge(cap, 1, Integer::sum);
            }
        }
        return capFreq.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .map(e -> new CapabilityPattern(e.getKey(), e.getValue(),
                        highPerformers.stream().filter(s -> s.getCapabilities().contains(e.getKey())).count()))
                .collect(Collectors.toList());
    }

    /** 提取在高成功率技能中出现 ≥ minOccurrence 次的公共能力词 */
    public List<String> extractCommonCapabilities(double minRate, int minOccurrence) {
        return analyzePatterns(minRate).stream()
                .filter(p -> p.frequency >= minOccurrence)
                .map(p -> p.capability)
                .collect(Collectors.toList());
    }

    // ---- 聚类 ----

    /** 按 capability 重叠度（Jaccard 相似度）聚类相似技能组。
     * @param minSimilarity Jaccard 阈值（0-1），超过此值的技能归为一组 */
    public List<SkillCluster> clusterSimilarSkills(double minSimilarity) {
        List<SkillDefinition> all = registry.getAllSkills();
        if (all.size() < 2) return List.of();

        // Union-Find 聚类
        int[] parent = new int[all.size()];
        for (int i = 0; i < all.size(); i++) parent[i] = i;

        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                double sim = jaccardSimilarity(all.get(i).getCapabilities(), all.get(j).getCapabilities());
                if (sim >= minSimilarity) union(parent, i, j);
            }
        }

        // 分组
        Map<Integer, List<SkillDefinition>> groups = new LinkedHashMap<>();
        for (int i = 0; i < all.size(); i++) {
            groups.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(all.get(i));
        }
        return groups.values().stream()
                .filter(g -> g.size() > 1) // 只保留多技能的聚类
                .map(g -> new SkillCluster(g, sharedCaps(g), avgSuccessRate(g)))
                .collect(Collectors.toList());
    }

    // ---- 工具方法 ----

    /** Jaccard 相似度 = |A ∩ B| / |A ∪ B| */
    static double jaccardSimilarity(List<String> a, List<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0;
        Set<String> setA = new HashSet<>(a), setB = new HashSet<>(b);
        Set<String> intersection = new HashSet<>(setA); intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA); union.addAll(setB);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private int find(int[] parent, int i) {
        while (parent[i] != i) { parent[i] = parent[parent[i]]; i = parent[i]; }
        return i;
    }
    private void union(int[] parent, int i, int j) {
        int ri = find(parent, i), rj = find(parent, j);
        if (ri != rj) parent[ri] = rj;
    }

    private List<String> sharedCaps(List<SkillDefinition> group) {
        if (group.isEmpty()) return List.of();
        Set<String> common = new HashSet<>(group.get(0).getCapabilities());
        for (int i = 1; i < group.size(); i++) common.retainAll(group.get(i).getCapabilities());
        return new ArrayList<>(common);
    }
    private double avgSuccessRate(List<SkillDefinition> group) {
        return group.stream().filter(s -> s.getUseCount() > 0)
                .mapToDouble(SkillDefinition::getSuccessRate).average().orElse(0);
    }

    // ---- 结果 DTO ----

    /** 能力词出现模式 */
    public record CapabilityPattern(String capability, int frequency, long distinctSkills) {}

    /** 相似技能聚类 */
    public record SkillCluster(List<SkillDefinition> members, List<String> sharedCapabilities, double avgSuccessRate) {
        public List<String> memberNames() {
            return members.stream().map(SkillDefinition::getName).collect(Collectors.toList());
        }
    }
}
