package org.xhy.domain.revision.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.revision.model.ReferencePaperEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

@Mapper
public interface ReferencePaperRepository extends MyBatisPlusExtRepository<ReferencePaperEntity> {
}
