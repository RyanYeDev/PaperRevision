package org.xhy.domain.tool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/** Skill技能注册中心 */
@Service
public class SkillRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    public SkillRegistry() {
        registerBuiltinSkills();
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

    /** Skill定义 */
    public static class SkillDefinition {
        private final String id;
        private final String name;
        private final String description;
        private final List<String> capabilities;

        public SkillDefinition(String id, String name, String description, List<String> capabilities) {
            this.id = id; this.name = name; this.description = description; this.capabilities = capabilities;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<String> getCapabilities() { return capabilities; }
    }
}
