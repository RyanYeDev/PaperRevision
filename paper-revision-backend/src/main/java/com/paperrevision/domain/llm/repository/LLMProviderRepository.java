package com.paperrevision.domain.llm.repository;

import org.apache.ibatis.annotations.Mapper;
import com.paperrevision.domain.llm.model.LLMProviderEntity;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

/** LLM提供商仓库 */
@Mapper
public interface LLMProviderRepository extends MyBatisPlusExtRepository<LLMProviderEntity> {
}
