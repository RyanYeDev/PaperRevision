package org.xhy.domain.paper.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.paper.model.PaperEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 论文仓库 */
@Mapper
public interface PaperRepository extends MyBatisPlusExtRepository<PaperEntity> {
}
