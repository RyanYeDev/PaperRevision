package com.paperrevision.domain.tool.service;

import com.paperrevision.domain.tool.model.SkillUsageEntity;
import com.paperrevision.domain.tool.repository.SkillUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/** Skill技能注册中心 */
@Service
public class SkillRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    /** 使用日志仓储，可选注入（单测无 Spring 时为 null，跳过入库） */
    @Autowired(required = false)
    private SkillUsageRepository usageRepository;

    public SkillRegistry() {
        registerBuiltinSkills();
    }

    public void setUsageRepository(SkillUsageRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    private void registerBuiltinSkills() {
        register(new SkillDefinition("academic_writing", "学术写作助手",
                "帮助改进论文的学术写作风格，使其更符合学术规范",
                List.of("improve_academic_tone", "enhance_clarity", "formalize_language")));

        register(new SkillDefinition("citation_format", "引用格式检查",
                "检查并修正论文中的引用格式，支持APA/MLA/Chicago等格式",
                List.of("check_citation_style", "format_references", "verify_citation_completeness")));

        register(new SkillDefinition("grammar_check", "语法检查",
                "检查英文语法错误、拼写错误和改进语言表达",
                List.of("check_grammar", "check_spelling", "suggest_improvements")));

        register(new SkillDefinition("structure_review", "结构审查",
                "审查论文结构是否合理，包括摘要/引言/方法/结果/讨论等部分",
                List.of("check_structure", "suggest_organization", "verify_imrad")));

        logger.info("内置Skill注册完成, 共{}个技能", skills.size());
    }

    public void register(SkillDefinition skill) {
        skills.put(skill.getId(), skill);
    }

    public SkillDefinition getSkill(String id) {
        return skills.get(id);
    }

    public List<SkillDefinition> getAllSkills() {
        return new ArrayList<>(skills.values());
    }

    public String getSkillsDescription() {
        StringBuilder sb = new StringBuilder();
        for (SkillDefinition skill : skills.values()) {
            sb.append("- ").append(skill.getName()).append(": ").append(skill.getDescription()).append("\n");
        }
        return sb.toString();
    }

    /** 记录 Skill 使用（简版，耗时/上下文规模默认 0） */
    public void recordUsage(String skillId, boolean success) {
        recordUsage(skillId, success, 0L, 0);
    }

    /** 记录 Skill 使用（含耗时与上下文规模），同步写库（若仓储可用） */
    public void recordUsage(String skillId, boolean success, long durationMs, int contextSize) {
        SkillDefinition skill = skills.get(skillId);
        if (skill == null) return;
        skill.useCount++;
        skill.lastUsedAt = System.currentTimeMillis();
        if (success) {
            skill.successCount++;
        }
        skill.successRate = skill.useCount > 0 ? (double) skill.successCount / skill.useCount : 0;
        logger.debug("Skill使用记录: {} success={} rate={}", skillId, success,
                String.format("%.0f%%", skill.successRate * 100));

        // 同步落库，供后续 Step3 推荐排序 / Step4 模式发现分析；入库失败不影响主流程
        if (usageRepository != null) {
            try {
                usageRepository.insert(new SkillUsageEntity(skillId, success, durationMs, contextSize));
            } catch (Exception e) {
                logger.warn("Skill使用日志入库失败: {} - {}", skillId, e.getMessage());
            }
        }
    }

    /** 获取使用最频繁的 Skill Top N */
    public List<SkillDefinition> getTopUsedSkills(int n) {
        return skills.values().stream()
                .sorted((a, b) -> Integer.compare(b.useCount, a.useCount))
                .limit(n).toList();
    }

    /** 获取成功率最高的 Skill Top N */
    public List<SkillDefinition> getTopPerformingSkills(int n) {
        return skills.values().stream()
                .filter(s -> s.useCount > 0)
                .sorted((a, b) -> Double.compare(b.successRate, a.successRate))
                .limit(n).toList();
    }

    /** 获取长时间未使用的 Skill（超过 thresholdMs 毫秒）*/
    public List<SkillDefinition> getStaleSkills(long thresholdMs) {
        long now = System.currentTimeMillis();
        return skills.values().stream()
                .filter(s -> s.lastUsedAt > 0 && (now - s.lastUsedAt) > thresholdMs)
                .toList();
    }

    /** Skill定义 */
    public static class SkillDefinition {
        private final String id;
        private final String name;
        private final String description;
        private final List<String> capabilities;
        /** 使用追踪 — Skill 自动进化的数据基础 */
        private int useCount = 0;
        private int successCount = 0;
        private double successRate = 0;
        private long lastUsedAt = 0;

        public SkillDefinition(String id, String name, String description, List<String> capabilities) {
            this.id = id; this.name = name; this.description = description; this.capabilities = capabilities;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<String> getCapabilities() { return capabilities; }
        public int getUseCount() { return useCount; }
        public int getSuccessCount() { return successCount; }
        public double getSuccessRate() { return successRate; }
        public long getLastUsedAt() { return lastUsedAt; }
    }
}
