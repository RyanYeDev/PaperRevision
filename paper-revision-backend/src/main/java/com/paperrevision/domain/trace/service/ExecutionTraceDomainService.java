package com.paperrevision.domain.trace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Agent执行链路追踪领域服务 */
@Service
public class ExecutionTraceDomainService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionTraceDomainService.class);

    // 内存存储 (生产环境应使用数据库)
    private final Map<String, List<TraceStep>> traces = new ConcurrentHashMap<>();

    /** 创建追踪会话 */
    public String createTrace(String userId, String sessionId, String agentId) {
        String traceId = UUID.randomUUID().toString();
        logger.info("创建追踪: traceId={}, sessionId={}, agentId={}", traceId, sessionId, agentId);
        return traceId;
    }

    /** 记录执行步骤 */
    public void recordStep(String traceId, String phase, String stepType, String input, String output) {
        TraceStep step = new TraceStep(traceId, phase, stepType, input, output, LocalDateTime.now());
        traces.computeIfAbsent(traceId, k -> new ArrayList<>()).add(step);
        logger.debug("记录步骤: {}, phase={}, stepType={}", traceId, phase, stepType);
    }

    /** 获取追踪记录 */
    public List<TraceStep> getTrace(String traceId) {
        return traces.getOrDefault(traceId, Collections.emptyList());
    }

    /** 追踪步骤 */
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
