package com.paperrevision.domain.evaluation.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.paperrevision.domain.evaluation.model.AgentExecutionTraceEntity;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

/** Agent执行追踪 Repository */
@Mapper
public interface AgentExecutionTraceRepository extends MyBatisPlusExtRepository<AgentExecutionTraceEntity> {

    /** 按会话ID查询所有追踪步骤，按创建时间排序 */
    @Select("SELECT * FROM agent_execution_traces WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<AgentExecutionTraceEntity> findBySessionId(@Param("sessionId") String sessionId);
}
