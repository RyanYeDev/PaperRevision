package com.paperrevision.interfaces.api.portal.evaluation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paperrevision.application.evaluation.dto.EvaluationDTO;
import com.paperrevision.application.evaluation.dto.TrajectoryEvalResultDTO;
import com.paperrevision.application.evaluation.service.EvaluationAppService;
import com.paperrevision.domain.evaluation.model.AgentExecutionTraceEntity;
import com.paperrevision.domain.evaluation.repository.AgentExecutionTraceRepository;
import com.paperrevision.domain.evaluation.service.EvaluationDomainService;
import com.paperrevision.interfaces.api.common.Result;

/** 评估控制器 */
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final EvaluationDomainService evaluationDomainService;
    private final EvaluationAppService evaluationAppService;
    private final AgentExecutionTraceRepository traceRepository;

    public EvaluationController(EvaluationDomainService evaluationDomainService,
            EvaluationAppService evaluationAppService,
            AgentExecutionTraceRepository traceRepository) {
        this.evaluationDomainService = evaluationDomainService;
        this.evaluationAppService = evaluationAppService;
        this.traceRepository = traceRepository;
    }

    /** 查询某返修结果的评估历史 */
    @GetMapping("/history/{resultId}")
    public Result<List<EvaluationDTO>> getHistory(@PathVariable String resultId) {
        List<EvaluationDTO> history = evaluationAppService.getEvaluationHistory(resultId);
        return Result.success(history);
    }

    /** 获取指定 session 的轨迹分析 */
    @GetMapping("/trajectory/{sessionId}")
    public Result<TrajectoryEvalResultDTO> getTrajectory(@PathVariable String sessionId) {
        List<AgentExecutionTraceEntity> traces = traceRepository.findBySessionId(sessionId);
        TrajectoryEvalResultDTO dto = evaluationAppService.getTrajectoryAnalysis(traces);
        return Result.success(dto);
    }
}
