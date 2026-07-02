package com.paperrevision.domain.rag.repository;

import org.apache.ibatis.annotations.Mapper;
import com.paperrevision.domain.rag.model.DocumentChunkEntity;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

/** 文档分块仓库 */
@Mapper
public interface DocumentChunkRepository extends MyBatisPlusExtRepository<DocumentChunkEntity> {
}
