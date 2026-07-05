package com.paperrevision.interfaces.api.portal.revision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import com.paperrevision.domain.evaluation.model.EvaluationEntity;
import com.paperrevision.domain.evaluation.service.EvaluationDomainService;
import com.paperrevision.domain.evaluation.service.RevisionEvaluator;
import com.paperrevision.domain.paper.model.PaperEntity;
import com.paperrevision.domain.paper.service.PaperDomainService;
import com.paperrevision.domain.rag.service.RagDomainService;
import com.paperrevision.domain.revision.service.RevisionExecutor;
import com.paperrevision.domain.revision.service.TextDiffEngine;
import com.paperrevision.domain.tool.service.SkillRegistry;
import com.paperrevision.domain.tool.service.ToolRegistry;
import com.paperrevision.domain.trace.service.ExecutionTraceDomainService;
import com.paperrevision.domain.workflow.service.WorkflowEngine;
import com.paperrevision.infrastructure.auth.UserContext;
import com.paperrevision.interfaces.api.common.Result;

import java.util.*;

/** 返修控制器 - 核心API */
@RestController
@RequestMapping("/api/revision")
public class RevisionController {

    private static final Logger logger = LoggerFactory.getLogger(RevisionController.class);

    private final RevisionExecutor revisionExecutor;
    private final PaperDomainService paperDomainService;
    private final RagDomainService ragDomainService;
    private final TextDiffEngine textDiffEngine;
    private final RevisionEvaluator revisionEvaluator;
    private final EvaluationDomainService evaluationDomainService;
    private final WorkflowEngine workflowEngine;
    private final ToolRegistry toolRegistry;
    private final SkillRegistry skillRegistry;
    private final ExecutionTraceDomainService traceService;

    public RevisionController(RevisionExecutor revisionExecutor, PaperDomainService paperDomainService,
            RagDomainService ragDomainService, TextDiffEngine textDiffEngine,
            RevisionEvaluator revisionEvaluator, EvaluationDomainService evaluationDomainService,
            WorkflowEngine workflowEngine, ToolRegistry toolRegistry, SkillRegistry skillRegistry,
            ExecutionTraceDomainService traceService) {
        this.revisionExecutor = revisionExecutor;
        this.paperDomainService = paperDomainService;
        this.ragDomainService = ragDomainService;
        this.textDiffEngine = textDiffEngine;
        this.revisionEvaluator = revisionEvaluator;
        this.evaluationDomainService = evaluationDomainService;
        this.workflowEngine = workflowEngine;
        this.toolRegistry = toolRegistry;
        this.skillRegistry = skillRegistry;
        this.traceService = traceService;
    }

    /** 执行论文返修 */
    @PostMapping("/execute")
    public Result<Map<String, Object>> executeRevision(@RequestBody Map<String, Object> request) {
        String userId = UserContext.getCurrentUserId();
        String paperId = (String) request.get("paperId");
        String referencePaperId = (String) request.get("referencePaperId");

        @SuppressWarnings("unchecked")
        List<String> comments = (List<String>) request.get("revisionComments");

        PaperEntity paper = paperDomainService.getPaperById(paperId);

        String traceId = traceService.createTrace(userId, paperId, "revision-agent");

        long startTime = System.currentTimeMillis();
        traceService.recordStep(traceId, "START", "revision_execution", "开始返修", "", null, 0, "SUCCESS");

        // 建立索引
        ragDomainService.indexDocument(paperId, paper.getParsedText());

        // 执行返修
        List<Map<String, Object>> revisionResults = revisionExecutor.executeRevision(
                paperId, paper.getParsedText(), comments, referencePaperId);

        // 评估（兼容旧接口的 Map 格式）
        Map<String, Object> evaluation = revisionEvaluator.evaluate(revisionResults.get(0));

        // 持久化评估结果（新增：轨迹分析 + 数据库存储）
        EvaluationEntity persistedEval = null;
        try {
            persistedEval = evaluationDomainService.evaluateRevision(
                    paperId, userId, revisionResults.get(0), traceId);
            // 用持久化的分数覆盖兼容评估的硬编码值
            evaluation.put("evaluationId", persistedEval.getId());
            evaluation.put("relevanceScore", persistedEval.getRelevanceScore());
            evaluation.put("faithfulnessScore", persistedEval.getFaithfulnessScore());
            evaluation.put("completenessScore", persistedEval.getCompletenessScore());
            evaluation.put("formatScore", persistedEval.getFormatScore());
            evaluation.put("overallScore", persistedEval.getOverallScore());
            evaluation.put("grade", persistedEval.getGrade());
            evaluation.put("evaluatorType", persistedEval.getEvaluatorType());
            evaluation.put("feedback", persistedEval.getFeedback());
        } catch (Exception e) {
            logger.warn("持久化评估失败，回退到兼容模式: {}", e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        int estimatedTokens = comments.size() * 500; // 粗略估算每个 comment 消耗约 500 tokens

        traceService.recordStep(traceId, "END", "revision_complete", "返修完成",
                evaluation.toString(), elapsed, estimatedTokens, "SUCCESS");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paperId", paperId);
        response.put("revisionResults", revisionResults);
        response.put("evaluation", evaluation);
        response.put("traceId", traceId);
        response.put("totalRequirements", comments.size());

        return Result.success("返修执行完成", response);
    }

    /** 生成文本差异 */
    @PostMapping("/diff")
    public Result<List<TextDiffEngine.DiffLine>> generateDiff(@RequestBody Map<String, String> request) {
        String original = request.get("originalText");
        String revised = request.get("revisedText");
        return Result.success(textDiffEngine.generateDiff(original, revised));
    }

    /** 获取工作流定义 */
    @GetMapping("/workflow")
    public Result<Object> getWorkflow() {
        return Result.success(workflowEngine.createPaperRevisionWorkflow());
    }

    /** 获取可用工具列表 */
    @GetMapping("/tools")
    public Result<List<Object>> getTools() {
        return Result.success(Collections.singletonList(
                Map.of("tools", toolRegistry.getToolsDescription(),
                        "skills", skillRegistry.getSkillsDescription())));
    }

    /** 获取执行追踪 */
    @GetMapping("/trace/{traceId}")
    public Result<List<ExecutionTraceDomainService.TraceStep>> getTrace(@PathVariable String traceId) {
        return Result.success(traceService.getTrace(traceId));
    }
}
