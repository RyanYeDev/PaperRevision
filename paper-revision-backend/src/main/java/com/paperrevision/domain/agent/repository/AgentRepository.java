package com.paperrevision.domain.agent.repository;

import org.apache.ibatis.annotations.Mapper;
import com.paperrevision.domain.agent.model.AgentEntity;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

@Mapper
public interface AgentRepository extends MyBatisPlusExtRepository<AgentEntity> {
}
