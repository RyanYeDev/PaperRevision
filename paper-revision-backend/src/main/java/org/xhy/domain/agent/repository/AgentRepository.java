package org.xhy.domain.agent.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

@Mapper
public interface AgentRepository extends MyBatisPlusExtRepository<AgentEntity> {
}
