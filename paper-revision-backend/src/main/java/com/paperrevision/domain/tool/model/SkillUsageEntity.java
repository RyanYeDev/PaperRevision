package com.paperrevision.domain.tool.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paperrevision.infrastructure.entity.BaseEntity;

/**
 * Skill 使用日志实体 — Skill 自动进化 Step 2 的数据基础
 *
 * 每次 Skill 被调用即落一条日志，为后续 Step 3(智能推荐排序)、
 * Step 4(成功模式发现)、Step 5(自动生成建议) 提供分析数据。
 */
@TableName("skill_usage_log")
public class SkillUsageEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String skillId;
    private Boolean success;
    private Long durationMs;
    private Integer contextSize;

    public SkillUsageEntity() {}

    public SkillUsageEntity(String skillId, Boolean success, Long durationMs, Integer contextSize) {
        this.skillId = skillId;
        this.success = success;
        this.durationMs = durationMs;
        this.contextSize = contextSize;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSkillId() { return skillId; }
    public void setSkillId(String skillId) { this.skillId = skillId; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Integer getContextSize() { return contextSize; }
    public void setContextSize(Integer contextSize) { this.contextSize = contextSize; }
}
