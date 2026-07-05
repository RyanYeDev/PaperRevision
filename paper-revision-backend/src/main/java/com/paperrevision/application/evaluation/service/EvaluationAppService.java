package com.paperrevision.application.evaluation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.paperrevision.application.evaluation.assembler.EvaluationAssembler;
import com.paperrevision.application.evaluation.dto.EvaluationDTO;
import com.paperrevision.application.evaluation.dto.TrajectoryEvalResultDTO;
import com.paperrevision.domain.evaluation.model.EvaluationEntity;
import com.paperrevision.domain.evaluation.model.TrajectoryEvalResult;
import com.paperrevision.domain.evaluation.service.EvaluationDomainService;
import com.paperrevision.domain.evaluation.service.TrajectoryAnalyzer;

/** 评估应用服务：编排领域服务并装配 DTO */
@Service
public class EvaluationAppService {

    private final EvaluationDomainService evaluationDomainService;
    private final TrajectoryAnalyzer trajectoryAnalyzer;

    public EvaluationAppService(EvaluationDomainService evaluationDomainService,
            TrajectoryAnalyzer trajectoryAnalyzer) {
        this.evaluationDomainService = evaluationDomainService;
        this.trajectoryAnalyzer = trajectoryAnalyzer;
    }

    /** 查询评估历史 */
    public List<EvaluationDTO> getEvaluationHistory(String revisionResultId) {
        List<EvaluationEntity> entities = evaluationDomainService.getEvaluationHistory(revisionResultId);
        return entities.stream().map(EvaluationAssembler::toDTO).toList();
    }

    /** 获取轨迹分析结果 */
    public TrajectoryEvalResultDTO getTrajectoryAnalysis(
            List<com.paperrevision.domain.evaluation.model.AgentExecutionTraceEntity> traces) {
        TrajectoryEvalResult result = trajectoryAnalyzer.analyze(traces);
        return EvaluationAssembler.toDTO(result);
    }
}
