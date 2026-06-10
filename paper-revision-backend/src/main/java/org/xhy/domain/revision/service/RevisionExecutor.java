package org.xhy.domain.revision.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.service.RetrievalService;
import org.xhy.domain.rag.model.DocumentChunkEntity;
import org.xhy.domain.tool.service.ToolRegistry;
import org.xhy.domain.workflow.service.WorkflowEngine;

import java.util.*;

/** 返修执行器 - 核心返修逻辑 */
@Service
public class RevisionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RevisionExecutor.class);

    private final WorkflowEngine workflowEngine;
    private final RetrievalService retrievalService;
    private final ToolRegistry toolRegistry;

    public RevisionExecutor(WorkflowEngine workflowEngine, RetrievalService retrievalService,
            ToolRegistry toolRegistry) {
        this.workflowEngine = workflowEngine;
        this.retrievalService = retrievalService;
        this.toolRegistry = toolRegistry;
    }

    /** 执行论文返修 */
    public List<Map<String, Object>> executeRevision(String paperId, String paperText,
            List<String> revisionComments, String referencePaperId) {

        logger.info("开始执行论文返修: paperId={}, 返修意见数={}", paperId, revisionComments.size());

        List<Map<String, Object>> results = new ArrayList<>();

        for (int i = 0; i < revisionComments.size(); i++) {
            String comment = revisionComments.get(i);
            logger.info("处理返修意见 #{}/{}: {}", i + 1, revisionComments.size(), comment.substring(0, Math.min(50, comment.length())));

            // RAG检索相关参考文献
            List<DocumentChunkEntity> relevantChunks = retrievalService.vectorSearch(comment, referencePaperId, 5);

            // 构建返修结果
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("requirementIndex", i + 1);
            result.put("requirement", comment);
            result.put("relevantReferences", relevantChunks.stream()
                    .map(c -> Map.of("content", c.getContent().substring(0, Math.min(200, c.getContent().length())), "index", c.getChunkIndex()))
                    .toList());
            result.put("suggestedRevision", "建议修改: [待LLM生成] " + comment);
            result.put("status", "COMPLETED");
            results.add(result);
        }

        logger.info("论文返修执行完成: {}条意见处理完毕", results.size());
        return results;
    }
}
