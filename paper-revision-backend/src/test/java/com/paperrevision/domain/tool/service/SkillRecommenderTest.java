package com.paperrevision.domain.tool.service;

import com.paperrevision.domain.tool.service.SkillRegistry.SkillDefinition;
import com.paperrevision.domain.tool.service.SkillRecommender.Recommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** SkillRecommender 单元测试 — Skill 自动进化 Step 3 */
class SkillRecommenderTest {

    private SkillRegistry registry;
    private SkillRecommender recommender;

    @BeforeEach
    void setUp() {
        registry = new SkillRegistry();
        recommender = new SkillRecommender(registry);
    }

    @Test
    void shouldRankBySuccessRate() {
        registry.recordUsage("grammar_check", true);
        registry.recordUsage("grammar_check", true);   // 100% 成功
        registry.recordUsage("citation_format", true);
        registry.recordUsage("citation_format", false); // 50% 成功

        List<Recommendation> recs = recommender.recommend(List.of(), 4);
        assertTrue(recs.size() >= 2);
        assertEquals("grammar_check", recs.get(0).skill().getId(),
                "100%成功率应排第一");
        assertTrue(recs.get(0).score() > recs.get(1).score());
    }

    @Test
    void coldStartSkillsShouldGetBaseScore() {
        List<Recommendation> recs = recommender.recommend(List.of(), 4);
        assertFalse(recs.isEmpty());
        double unusedScore = recs.stream()
                .filter(r -> r.skill().getUseCount() == 0)
                .findFirst().orElseThrow().score();
        assertTrue(unusedScore >= 0.15, "冷启动评分不应为 0, got " + unusedScore);
    }

    @Test
    void keywordMatchShouldBoostRanking() {
        registry.recordUsage("citation_format", true); // 低使用也无所谓
        registry.recordUsage("grammar_check", true);

        // citation: 引用格式检查, capabilities 含 "format_references"
        List<Recommendation> recs = recommender.recommend(List.of("引用", "格式"), 4);
        assertEquals("citation_format", recs.get(0).skill().getId(),
                "关键词 '引用'或'格式' 应置顶 citation_format");
    }

    @Test
    void shouldRespectTopN() {
        assertEquals(2, recommender.recommend(List.of(), 2).size());
        assertEquals(4, recommender.recommend(List.of(), 4).size());
    }

    @Test
    void nullKeywordsShouldTreatAsEmpty() {
        assertDoesNotThrow(() -> recommender.recommend(null, 4));
        assertFalse(recommender.recommend(null, 4).isEmpty());
    }

    @Test
    void capabilityMatchShouldWork() {
        registry.recordUsage("academic_writing", true);
        // academic_writing 的 capabilities 含 "enhance_clarity"
        List<Recommendation> recs = recommender.recommend(List.of("clarity"), 4);
        assertEquals("academic_writing", recs.get(0).skill().getId());
    }
}
