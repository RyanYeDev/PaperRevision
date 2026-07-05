package com.paperrevision.application.evaluation.dto;

import java.time.LocalDateTime;

/** 评估结果 DTO */
public class EvaluationDTO {

    private String id;
    private String revisionResultId;
    private Double relevanceScore;
    private Double faithfulnessScore;
    private Double completenessScore;
    private Double formatScore;
    private Double overallScore;
    private String feedback;
    private String evaluatorType;
    private String grade;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRevisionResultId() { return revisionResultId; }
    public void setRevisionResultId(String revisionResultId) { this.revisionResultId = revisionResultId; }

    public Double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }

    public Double getFaithfulnessScore() { return faithfulnessScore; }
    public void setFaithfulnessScore(Double faithfulnessScore) { this.faithfulnessScore = faithfulnessScore; }

    public Double getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(Double completenessScore) { this.completenessScore = completenessScore; }

    public Double getFormatScore() { return formatScore; }
    public void setFormatScore(Double formatScore) { this.formatScore = formatScore; }

    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getEvaluatorType() { return evaluatorType; }
    public void setEvaluatorType(String evaluatorType) { this.evaluatorType = evaluatorType; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
