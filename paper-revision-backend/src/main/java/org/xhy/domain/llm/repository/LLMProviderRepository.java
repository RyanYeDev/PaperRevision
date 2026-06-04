package org.xhy.domain.llm.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.llm.model.LLMProviderEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** LLM提供商仓库 */
@Mapper
public interface LLMProviderRepository extends MyBatisPlusExtRepository<LLMProviderEntity> {
}
