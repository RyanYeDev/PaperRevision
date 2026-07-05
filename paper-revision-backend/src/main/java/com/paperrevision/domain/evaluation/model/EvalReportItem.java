package com.paperrevision.domain.evaluation.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paperrevision.infrastructure.entity.BaseEntity;

/** 单条用例的评估结果（属于 EvaluationReport 聚合） */
@TableName("agent_eval_report_items")
public class EvalReportItem extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String reportId;
    private String caseId;
    private Double overallScore;
    private Double trajectoryScore;
    private Double llmScore;
    private Boolean passed;
    private String feedback;
    private String traceId;
    private Long durationMs;
    private Integer tokensUsed;
    private String detailsJson;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }

    public Double getTrajectoryScore() { return trajectoryScore; }
    public void setTrajectoryScore(Double trajectoryScore) { this.trajectoryScore = trajectoryScore; }

    public Double getLlmScore() { return llmScore; }
    public void setLlmScore(Double llmScore) { this.llmScore = llmScore; }

    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }

    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
}
