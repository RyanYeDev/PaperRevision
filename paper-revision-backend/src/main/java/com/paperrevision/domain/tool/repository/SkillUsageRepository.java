package com.paperrevision.domain.tool.repository;

import org.apache.ibatis.annotations.Mapper;
import com.paperrevision.domain.tool.model.SkillUsageEntity;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

/** Skill 使用日志仓储 — Skill 自动进化 Step 2 */
@Mapper
public interface SkillUsageRepository extends MyBatisPlusExtRepository<SkillUsageEntity> {
}
