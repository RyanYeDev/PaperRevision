package com.paperrevision.domain.tool.service;

import com.paperrevision.domain.tool.model.SkillUsageEntity;
import com.paperrevision.domain.tool.repository.SkillUsageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** SkillRegistry 使用数据持久化测试 — Skill 自动进化 Step 2 */
class SkillRegistryPersistenceTest {

    @Test
    void recordUsageShouldPersistLogWhenRepositoryPresent() {
        SkillRegistry registry = new SkillRegistry();
        SkillUsageRepository repo = mock(SkillUsageRepository.class);
        registry.setUsageRepository(repo);

        registry.recordUsage("grammar_check", true, 1200L, 3000);

        ArgumentCaptor<SkillUsageEntity> captor = ArgumentCaptor.forClass(SkillUsageEntity.class);
        verify(repo, times(1)).insert(captor.capture());
        SkillUsageEntity saved = captor.getValue();
        assertEquals("grammar_check", saved.getSkillId());
        assertTrue(saved.getSuccess());
        assertEquals(1200L, saved.getDurationMs());
        assertEquals(3000, saved.getContextSize());
    }

    @Test
    void recordUsageShouldStillUpdateInMemoryStats() {
        SkillRegistry registry = new SkillRegistry();
        registry.setUsageRepository(mock(SkillUsageRepository.class));

        registry.recordUsage("grammar_check", true, 0L, 0);
        registry.recordUsage("grammar_check", false, 0L, 0);

        SkillRegistry.SkillDefinition skill = registry.getSkill("grammar_check");
        assertEquals(2, skill.getUseCount());
        assertEquals(1, skill.getSuccessCount());
        assertEquals(0.5, skill.getSuccessRate(), 1e-9);
    }

    @Test
    void recordUsageShouldNotThrowWhenNoRepository() {
        SkillRegistry registry = new SkillRegistry(); // 无仓储注入
        assertDoesNotThrow(() -> registry.recordUsage("grammar_check", true, 500L, 100));
        assertEquals(1, registry.getSkill("grammar_check").getUseCount());
    }

    @Test
    void twoArgRecordUsageShouldDelegateWithZeroDefaults() {
        SkillRegistry registry = new SkillRegistry();
        SkillUsageRepository repo = mock(SkillUsageRepository.class);
        registry.setUsageRepository(repo);

        registry.recordUsage("grammar_check", true);

        ArgumentCaptor<SkillUsageEntity> captor = ArgumentCaptor.forClass(SkillUsageEntity.class);
        verify(repo).insert(captor.capture());
        assertEquals(0L, captor.getValue().getDurationMs());
        assertEquals(0, captor.getValue().getContextSize());
    }

    @Test
    void unknownSkillShouldNotPersist() {
        SkillRegistry registry = new SkillRegistry();
        SkillUsageRepository repo = mock(SkillUsageRepository.class);
        registry.setUsageRepository(repo);

        registry.recordUsage("nonexistent_skill", true, 100L, 10);

        verify(repo, never()).insert(any(SkillUsageEntity.class));
    }

    @Test
    void persistFailureShouldNotBreakRecording() {
        SkillRegistry registry = new SkillRegistry();
        SkillUsageRepository repo = mock(SkillUsageRepository.class);
        when(repo.insert(any(SkillUsageEntity.class))).thenThrow(new RuntimeException("DB 挂了"));
        registry.setUsageRepository(repo);

        assertDoesNotThrow(() -> registry.recordUsage("grammar_check", true, 100L, 10));
        assertEquals(1, registry.getSkill("grammar_check").getUseCount());
    }
}
