package com.paperrevision.domain.tool.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** SkillRegistry 使用追踪单元测试 */
class SkillRegistryTest {

    private SkillRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SkillRegistry();
    }

    @Test
    void shouldHaveBuiltinSkills() {
        List<SkillRegistry.SkillDefinition> all = registry.getAllSkills();
        assertEquals(4, all.size(), "应有 4 个内置 Skill");
    }

    @Test
    void shouldRecordUsageAndTrackSuccess() {
        registry.recordUsage("grammar_check", true);
        registry.recordUsage("grammar_check", false);
        registry.recordUsage("grammar_check", true);

        SkillRegistry.SkillDefinition skill = registry.getSkill("grammar_check");
        assertEquals(3, skill.getUseCount());
        assertEquals(2, skill.getSuccessCount());
        assertTrue(skill.getSuccessRate() > 0.66, "成功率应 >66%");
        assertTrue(skill.getLastUsedAt() > 0, "应记录最后使用时间");
    }

    @Test
    void shouldIgnoreUnknownSkill() {
        registry.recordUsage("unknown_skill", true);
        // 不应抛异常
    }

    @Test
    void shouldReturnTopUsedSkills() {
        registry.recordUsage("grammar_check", true);
        registry.recordUsage("grammar_check", true);
        registry.recordUsage("citation_format", true);

        List<SkillRegistry.SkillDefinition> top = registry.getTopUsedSkills(2);
        assertEquals(2, top.size());
        assertEquals("grammar_check", top.get(0).getId());
        assertTrue(top.get(0).getUseCount() >= top.get(1).getUseCount());
    }

    @Test
    void shouldFilterZeroUsageFromTopPerforming() {
        // 未使用的 Skill 成功率=0，应被过滤
        List<SkillRegistry.SkillDefinition> top = registry.getTopPerformingSkills(10);
        assertTrue(top.isEmpty(), "零使用的 Skill 不应出现在排行榜");
    }

    @Test
    void shouldReturnStaleSkills() {
        registry.recordUsage("grammar_check", true);
        // 其他 3 个未使用，lastUsedAt=0，不应被识别为 stale
        List<SkillRegistry.SkillDefinition> stale = registry.getStaleSkills(3600_000);
        assertTrue(stale.isEmpty(), "从未使用的 Skill 不应被识别为 stale");
    }

    @Test
    void shouldGetSkillsDescription() {
        String desc = registry.getSkillsDescription();
        assertTrue(desc.contains("学术写作助手"));
        assertTrue(desc.contains("引用格式检查"));
        assertTrue(desc.contains("语法检查"));
        assertTrue(desc.contains("结构审查"));
    }
}
