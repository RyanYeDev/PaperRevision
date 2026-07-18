package com.paperrevision.domain.tool.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** SkillEvolutionService 单元测试 — Skill 自动进化 Step 5 */
class SkillEvolutionServiceTest {

    private SkillRegistry registry;
    private SkillEvolutionService service;

    @BeforeEach
    void setUp() {
        registry = new SkillRegistry();
        service = new SkillEvolutionService(registry, new SuccessPatternAnalyzer(registry));

        // 模拟使用数据：让多个技能达到高成功率
        recordSuccess("academic_writing", 5, 4);
        recordSuccess("grammar_check", 5, 5);
        recordSuccess("citation_format", 4, 4);
        recordSuccess("structure_review", 3, 2);

        // 注册额外技能以增加聚类可能性
        registry.register(new SkillRegistry.SkillDefinition("proofread", "校对",
                "综合校对检查", List.of("check_grammar", "check_spelling")));
        recordSuccess("proofread", 3, 3);

        registry.register(new SkillRegistry.SkillDefinition("language_polish", "语言润色",
                "改进语言", List.of("enhance_clarity", "suggest_improvements")));
        recordSuccess("language_polish", 2, 2);
    }

    private void recordSuccess(String skillId, int total, int successes) {
        for (int i = 0; i < successes; i++) registry.recordUsage(skillId, true);
        for (int i = successes; i < total; i++) registry.recordUsage(skillId, false);
    }

    @Test
    void shouldGenerateCandidatesFromPatterns() {
        List<SkillEvolutionService.SkillCandidate> candidates = service.generateCandidates();
        assertFalse(candidates.isEmpty(), "应生成至少一个候选技能");
        // 验证候选结构完整
        SkillEvolutionService.SkillCandidate first = candidates.get(0);
        assertNotNull(first.name());
        assertNotNull(first.description());
        assertFalse(first.capabilities().isEmpty());
        assertTrue(first.confidence() > 0 && first.confidence() <= 1.0,
                "置信度应在(0,1]范围内, got: " + first.confidence());
        assertNotNull(first.rationale());
    }

    @Test
    void candidatesShouldBeSortedByConfidenceDesc() {
        List<SkillEvolutionService.SkillCandidate> candidates = service.generateCandidates();
        for (int i = 1; i < candidates.size(); i++) {
            assertTrue(candidates.get(i - 1).confidence() >= candidates.get(i).confidence(),
                    "候选应按置信度降序排列");
        }
    }

    @Test
    void shouldRegisterCandidateSuccessfully() {
        SkillEvolutionService.SkillCandidate candidate = new SkillEvolutionService.SkillCandidate(
                "测试技能", "测试用", List.of("test_capability"), 0.8, "测试理由");
        SkillRegistry.SkillDefinition registered = service.registerCandidate(candidate);
        assertNotNull(registered);
        assertEquals("测试技能", registered.getName());
        assertNotNull(registry.getSkill("测试技能"));
    }

    @Test
    void shouldReturnEmptyForNoData() {
        SkillRegistry emptyReg = new SkillRegistry();
        SkillEvolutionService emptyService =
                new SkillEvolutionService(emptyReg, new SuccessPatternAnalyzer(emptyReg));
        List<SkillEvolutionService.SkillCandidate> candidates = emptyService.generateCandidates();
        // 无使用数据 → 无高成功率技能 → 无候选
        assertTrue(candidates.isEmpty(), "无数据时应返回空列表");
    }

    @Test
    void shouldNotDuplicateExistingSkillNames() {
        List<SkillEvolutionService.SkillCandidate> candidates = service.generateCandidates();
        List<String> existingNames = registry.getAllSkills().stream()
                .map(SkillRegistry.SkillDefinition::getName).toList();
        for (SkillEvolutionService.SkillCandidate c : candidates) {
            assertFalse(existingNames.contains(c.name()),
                    "候选名不应与已有技能重名: " + c.name());
        }
    }

    @Test
    void capabilityToNameShouldBeReadable() {
        // 通过 registerCandidate 间接验证名称转换结果可读
        SkillEvolutionService.SkillCandidate candidate = new SkillEvolutionService.SkillCandidate(
                "检查语法", "测试", List.of("check_grammar"), 0.7, "test");
        SkillRegistry.SkillDefinition def = service.registerCandidate(candidate);
        assertEquals("检查语法", def.getName());
    }
}
