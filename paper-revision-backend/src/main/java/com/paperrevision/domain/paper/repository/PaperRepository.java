package com.paperrevision.domain.paper.repository;

import org.apache.ibatis.annotations.Mapper;
import com.paperrevision.domain.paper.model.PaperEntity;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

/** 论文仓库 */
@Mapper
public interface PaperRepository extends MyBatisPlusExtRepository<PaperEntity> {
}
