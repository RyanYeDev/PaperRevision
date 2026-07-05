package com.paperrevision.domain.evaluation.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paperrevision.infrastructure.entity.BaseEntity;

/** 评估记录实体，映射 evaluations 表 */
@TableName("evaluations")
public class EvaluationEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String revisionResultId;
    private Double relevanceScore;
    private Double faithfulnessScore;
    private Double completenessScore;
    private Double formatScore;
    private Double overallScore;
    private String feedback;
    private String evaluatorType;
    private String userId;

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

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGrade() {
        if (overallScore == null) return "未评估";
        if (overallScore >= 0.8) return "优秀";
        if (overallScore >= 0.6) return "良好";
        return "需改进";
    }
}
