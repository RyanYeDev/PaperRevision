package com.paperrevision.domain.revision.repository;

import org.apache.ibatis.annotations.Mapper;
import com.paperrevision.domain.revision.model.ReferencePaperEntity;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

@Mapper
public interface ReferencePaperRepository extends MyBatisPlusExtRepository<ReferencePaperEntity> {
}
