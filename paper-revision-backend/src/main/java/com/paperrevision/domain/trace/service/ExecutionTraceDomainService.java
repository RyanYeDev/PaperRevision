package com.paperrevision.domain.trace.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.paperrevision.domain.evaluation.model.AgentExecutionTraceEntity;
import com.paperrevision.domain.evaluation.repository.AgentExecutionTraceRepository;
import com.paperrevision.infrastructure.auth.UserContext;

/** Agent执行链路追踪领域服务（DB 持久化，替代原先的内存存储） */
@Service
public class ExecutionTraceDomainService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionTraceDomainService.class);

    private final AgentExecutionTraceRepository traceRepository;

    public ExecutionTraceDomainService(AgentExecutionTraceRepository traceRepository) {
        this.traceRepository = traceRepository;
    }

    /** 创建追踪会话 */
    public String createTrace(String userId, String sessionId, String agentId) {
        String traceId = UUID.randomUUID().toString();
        logger.info("创建追踪: traceId={}, sessionId={}, agentId={}", traceId, sessionId, agentId);
        return traceId;
    }

    /** 记录执行步骤（简化版，兼容旧接口） */
    public void recordStep(String traceId, String phase, String stepType, String input, String output) {
        recordStep(traceId, phase, stepType, input, output, null, null, "SUCCESS");
    }

    /** 记录执行步骤（完整版，包含耗时和 Token） */
    public void recordStep(String traceId, String phase, String stepType, String input, String output,
            Long durationMs, Integer tokensUsed, String status) {
        AgentExecutionTraceEntity entity = new AgentExecutionTraceEntity();
        entity.setSessionId(traceId);
        entity.setPhase(phase);
        entity.setStepType(stepType);
        entity.setInputData(input);
        entity.setOutputData(output);
        entity.setDurationMs(durationMs);
        entity.setTokensUsed(tokensUsed);
        entity.setToolCalls(1);
        entity.setModelCalls(1);
        entity.setStatus(status);
        entity.setUserId(UserContext.getCurrentUserId());
        entity.setAgentId("revision-agent");
        traceRepository.checkInsert(entity);
        logger.debug("记录步骤: {}, phase={}, stepType={}, durationMs={}, tokens={}",
                traceId, phase, stepType, durationMs, tokensUsed);
    }

    /** 获取追踪记录（兼容旧接口，返回 DTO） */
    public List<TraceStep> getTrace(String traceId) {
        List<AgentExecutionTraceEntity> entities = traceRepository.findBySessionId(traceId);

        // 兼容逻辑：如果 traceId 作为 sessionId 没找到，可能是旧版随机 traceId
        // 这里直接用 sessionId 查询
        if (entities.isEmpty()) {
            logger.warn("未找到追踪记录: sessionId={}", traceId);
            return Collections.emptyList();
        }

        return entities.stream()
                .map(e -> new TraceStep(e.getSessionId(), e.getPhase(), e.getStepType(),
                        e.getInputData(), e.getOutputData(), e.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /** 获取持久化的追踪实体列表 */
    public List<AgentExecutionTraceEntity> getTraceEntities(String sessionId) {
        return traceRepository.findBySessionId(sessionId);
    }

    /** 追踪步骤（DTO，保持 API 兼容性） */
    public static class TraceStep {
        private final String traceId;
        private final String phase;
        private final String stepType;
        private final String input;
        private final String output;
        private final LocalDateTime timestamp;

        public TraceStep(String traceId, String phase, String stepType, String input, String output,
                LocalDateTime timestamp) {
            this.traceId = traceId;
            this.phase = phase;
            this.stepType = stepType;
            this.input = input;
            this.output = output;
            this.timestamp = timestamp;
        }

        public String getTraceId() { return traceId; }
        public String getPhase() { return phase; }
        public String getStepType() { return stepType; }
        public String getInput() { return input; }
        public String getOutput() { return output; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
