package com.paperrevision.domain.tool.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** SuccessPatternAnalyzer 单元测试 — Skill 自动进化 Step 4 */
class SuccessPatternAnalyzerTest {

    private SkillRegistry registry;
    private SuccessPatternAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        registry = new SkillRegistry();
        analyzer = new SuccessPatternAnalyzer(registry);

        // 模拟使用数据：academic_writing 高成功率，grammar_check 中等，structure_review 未用
        registry.recordUsage("academic_writing", true);
        registry.recordUsage("academic_writing", true);
        registry.recordUsage("academic_writing", true);
        registry.recordUsage("academic_writing", false); // 3/4 = 75%

        registry.recordUsage("grammar_check", true);
        registry.recordUsage("grammar_check", false); // 1/2 = 50%

        registry.recordUsage("citation_format", true);
        registry.recordUsage("citation_format", true); // 2/2 = 100%

        // structure_review 零使用
    }

    @Test
    void shouldFindHighSuccessPatterns() {
        List<SuccessPatternAnalyzer.CapabilityPattern> patterns = analyzer.analyzePatterns(0.7);
        assertFalse(patterns.isEmpty(), "应有成功率 ≥70% 的技能产生模式");

        // citation_format(100%) 和 academic_writing(75%) 的能力词应上榜
        List<String> caps = patterns.stream().map(p -> p.capability()).toList();
        assertTrue(caps.contains("improve_academic_tone") || caps.contains("check_citation_style"),
                "高成功率技能的能力词应出现");
    }

    @Test
    void shouldExtractCommonCapabilities() {
        List<String> common = analyzer.extractCommonCapabilities(0.6, 1);
        assertFalse(common.isEmpty(), "成功率 ≥60% 的技能应有 ≥1 条公共能力");
    }

    @Test
    void shouldReturnEmptyWhenNoData() {
        // 清空数据，所有技能零使用 → 无高成功率记录
        SkillRegistry emptyReg = new SkillRegistry();
        SuccessPatternAnalyzer emptyAnalyzer = new SuccessPatternAnalyzer(emptyReg);
        assertTrue(emptyAnalyzer.analyzePatterns(0.8).isEmpty());
        assertTrue(emptyAnalyzer.extractCommonCapabilities(0.8, 2).isEmpty());
    }

    @Test
    void jaccardShouldComputeCorrectly() {
        assertEquals(1.0, SuccessPatternAnalyzer.jaccardSimilarity(
                List.of("a", "b"), List.of("a", "b")));
        assertEquals(0.0, SuccessPatternAnalyzer.jaccardSimilarity(
                List.of("a"), List.of("b")));
        assertEquals(1.0 / 3.0, SuccessPatternAnalyzer.jaccardSimilarity(
                List.of("a", "b"), List.of("a", "c")), 0.001);
        // 空集合
        assertEquals(0.0, SuccessPatternAnalyzer.jaccardSimilarity(
                List.of(), List.of()));
    }

    @Test
    void shouldClusterSimilarSkills() {
        // 注册两个能力重叠高的技能
        registry.register(new SkillRegistry.SkillDefinition("proofread", "校对",
                "综合校对", List.of("check_grammar", "check_spelling", "suggest_improvements")));
        registry.register(new SkillRegistry.SkillDefinition("language_polish", "语言润色",
                "改进语言表达", List.of("check_grammar", "enhance_clarity", "suggest_improvements")));

        List<SuccessPatternAnalyzer.SkillCluster> clusters = analyzer.clusterSimilarSkills(0.2);
        assertFalse(clusters.isEmpty(), "有重叠能力的技能应聚类, got: " + clusters.size() + " clusters");
        // 至少应有一个包含 grammar_check + proofread + language_polish 的聚类
        boolean found = clusters.stream().anyMatch(c ->
                c.memberNames().contains("语法检查") && c.memberNames().contains("校对"));
        assertTrue(found, "grammar_check 与 proofread 应因 check_grammar 能力重叠而聚类");
    }

    @Test
    void shouldReturnEmptyClustersWhenBelowThreshold() {
        List<SuccessPatternAnalyzer.SkillCluster> clusters = analyzer.clusterSimilarSkills(1.0);
        // 阈值 1.0 = 完全相同才聚类，内置技能 capabilities 不完全相同
        assertTrue(clusters.isEmpty(), "阈值为 1.0 时应无聚类");
    }

    @Test
    void nullSafetyForRepositoryAbsence() {
        // analyzer 未注入 usageRepository，所有方法应降级不抛异常
        assertDoesNotThrow(() -> analyzer.analyzePatterns(0.5));
        assertDoesNotThrow(() -> analyzer.extractCommonCapabilities(0.5, 1));
        assertDoesNotThrow(() -> analyzer.clusterSimilarSkills(0.5));
    }
}
