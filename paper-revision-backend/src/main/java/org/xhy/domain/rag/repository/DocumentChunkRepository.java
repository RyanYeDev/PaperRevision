package org.xhy.domain.rag.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.rag.model.DocumentChunkEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 文档分块仓库 */
@Mapper
public interface DocumentChunkRepository extends MyBatisPlusExtRepository<DocumentChunkEntity> {
}
