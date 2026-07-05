package com.paperrevision.application.evaluation.assembler;

import com.paperrevision.application.evaluation.dto.EvaluationDTO;
import com.paperrevision.application.evaluation.dto.TrajectoryEvalResultDTO;
import com.paperrevision.domain.evaluation.model.EvaluationEntity;
import com.paperrevision.domain.evaluation.model.TrajectoryEvalResult;

/** 评估 Assembler：Entity ↔ DTO 转换 */
public class EvaluationAssembler {

    private EvaluationAssembler() {}

    public static EvaluationDTO toDTO(EvaluationEntity entity) {
        if (entity == null) return null;
        EvaluationDTO dto = new EvaluationDTO();
        dto.setId(entity.getId());
        dto.setRevisionResultId(entity.getRevisionResultId());
        dto.setRelevanceScore(entity.getRelevanceScore());
        dto.setFaithfulnessScore(entity.getFaithfulnessScore());
        dto.setCompletenessScore(entity.getCompletenessScore());
        dto.setFormatScore(entity.getFormatScore());
        dto.setOverallScore(entity.getOverallScore());
        dto.setFeedback(entity.getFeedback());
        dto.setEvaluatorType(entity.getEvaluatorType());
        dto.setGrade(entity.getGrade());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public static TrajectoryEvalResultDTO toDTO(TrajectoryEvalResult result) {
        if (result == null) return null;
        TrajectoryEvalResultDTO dto = new TrajectoryEvalResultDTO();
        dto.setStepEfficiency(result.getStepEfficiency());
        dto.setToolAccuracy(result.getToolAccuracy());
        dto.setParamCorrectness(result.getParamCorrectness());
        dto.setRedundancyRate(result.getRedundancyRate());
        dto.setTotalDurationMs(result.getTotalDurationMs());
        dto.setTotalTokens(result.getTotalTokens());
        dto.setSuccessRate(result.getSuccessRate());
        dto.setTotalModelCalls(result.getTotalModelCalls());
        dto.setTotalSteps(result.getTotalSteps());
        dto.setSuccessSteps(result.getSuccessSteps());
        dto.setEffectiveToolCalls(result.getEffectiveToolCalls());
        dto.setTotalToolCalls(result.getTotalToolCalls());
        return dto;
    }
}
