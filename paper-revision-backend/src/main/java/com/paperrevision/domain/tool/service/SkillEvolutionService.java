package com.paperrevision.domain.tool.service;

import com.paperrevision.domain.tool.service.SkillRegistry.SkillDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Skill 进化服务 — Skill 自动进化 Step 5
 *
 * 分析现有技能的使用数据与成功模式，自动生成候选新 Skill 建议。
 * 候选经人工审核后可一键注册到 {@link SkillRegistry}。
 *
 * <p>依赖 Step3({@link SkillRecommender}) 的评分逻辑 +
 * Step4({@link SuccessPatternAnalyzer}) 的模式发现，产出可操作的进化建议。
 */
@Service
public class SkillEvolutionService {

    private static final Logger logger = LoggerFactory.getLogger(SkillEvolutionService.class);

    private final SkillRegistry registry;
    private final SuccessPatternAnalyzer analyzer;

    public SkillEvolutionService(SkillRegistry registry, SuccessPatternAnalyzer analyzer) {
        this.registry = registry;
        this.analyzer = analyzer;
    }

    /**
     * 生成候选新 Skill 建议。
     * 策略：① 高成功率能力词中未充分覆盖的 → 建议封装为新技能；
     *       ② 技能聚类间缺失桥接技能的 → 建议中间层技能。
     */
    public List<SkillCandidate> generateCandidates() {
        List<SkillCandidate> candidates = new ArrayList<>();

        // 策略①：能力缺口 — 高频出现但仅被少数技能覆盖
        List<SuccessPatternAnalyzer.CapabilityPattern> patterns = analyzer.analyzePatterns(0.6);
        Set<String> existingCaps = registry.getAllSkills().stream()
                .flatMap(s -> s.getCapabilities().stream())
                .collect(Collectors.toSet());
        Set<String> existingNames = registry.getAllSkills().stream()
                .map(SkillDefinition::getName).collect(Collectors.toSet());

        for (SuccessPatternAnalyzer.CapabilityPattern p : patterns) {
            if (!existingCaps.contains(p.capability())) continue;
            if (p.distinctSkills() >= 2) {
                // 该能力词被 3+ 个技能共享 → 可能值得独立为一个新技能
                String suggestedName = capabilityToName(p.capability());
                if (!existingNames.contains(suggestedName)) {
                    candidates.add(new SkillCandidate(
                            suggestedName,
                            "自动生成建议：基于高成功率能力词「" + p.capability() + "」推导",
                            List.of(p.capability()),
                            Math.min(0.9, 0.5 + p.frequency() * 0.1),
                            "能力词「" + p.capability() + "」出现在 " +
                                    p.distinctSkills() + " 个高成功率技能中"));
                }
            }
        }

        // 策略②：聚类间桥接 — 两个聚类之间缺少覆盖的能力交集
        List<SuccessPatternAnalyzer.SkillCluster> clusters = analyzer.clusterSimilarSkills(0.2);
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = i + 1; j < clusters.size(); j++) {
                Set<String> union = new HashSet<>(clusters.get(i).sharedCapabilities());
                union.addAll(clusters.get(j).sharedCapabilities());
                Set<String> intersection = new HashSet<>(clusters.get(i).sharedCapabilities());
                intersection.retainAll(clusters.get(j).sharedCapabilities());
                union.removeAll(intersection); // 差异能力
                if (union.size() >= 2 && !intersection.isEmpty()) {
                    String name = "综合" + intersection.iterator().next().replace("_", "");
                    if (!existingNames.contains(name)) {
                        candidates.add(new SkillCandidate(name,
                                "桥接聚类「" + String.join(",", clusters.get(i).memberNames()) +
                                        "」与「" + String.join(",", clusters.get(j).memberNames()) + "」",
                                new ArrayList<>(union),
                                0.55, "两个相关技能聚类存在能力差异，可桥接"));
                    }
                }
            }
        }

        logger.info("Skill进化分析完成: 生成{}个候选", candidates.size());
        return candidates.stream()
                .sorted(Comparator.comparingDouble(SkillCandidate::confidence).reversed())
                .collect(Collectors.toList());
    }

    /** 将候选 Skill 注册到 Registry（需人工审核后调用） */
    public SkillDefinition registerCandidate(SkillCandidate candidate) {
        SkillDefinition def = new SkillDefinition(
                candidate.name().toLowerCase().replaceAll("\\s+", "_"),
                candidate.name(), candidate.description(), candidate.capabilities());
        registry.register(def);
        logger.info("候选Skill已注册: {}", candidate.name());
        return def;
    }

    /** 将 capability 下划线命名转为中文技能名 */
    private String capabilityToName(String cap) {
        return cap.replace("check_grammar", "语法检查").replace("check_spelling", "拼写检查")
                .replace("check_structure", "结构检查").replace("check_citation_style", "引用格式检查")
                .replace("improve_academic_tone", "学术语调优化")
                .replace("enhance_clarity", "表达清晰度增强")
                .replace("formalize_language", "语言规范化")
                .replace("format_references", "参考文献格式化")
                .replace("verify_citation_completeness", "引用完整性验证")
                .replace("verify_imrad", "IMRaD结构验证")
                .replace("suggest_improvements", "改进建议").replace("suggest_organization", "结构优化建议")
                .replace("check_", "检查").replace("improve_", "改进")
                .replace("enhance_", "增强").replace("format_", "格式化")
                .replace("verify_", "验证").replace("suggest_", "建议")
                .replace("_", "");
    }

    /** 候选 Skill */
    public record SkillCandidate(String name, String description,
                                  List<String> capabilities, double confidence, String rationale) {}
}
