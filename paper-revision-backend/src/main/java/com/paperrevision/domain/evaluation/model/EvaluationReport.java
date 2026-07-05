package com.paperrevision.domain.evaluation.model;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paperrevision.infrastructure.entity.BaseEntity;

/**
 * 评估报告 - 聚合根
 * 管理测试套件批量运行结果的聚合和统计
 *
 * DDD 设计：EvaluationReport 是聚合根，EvalReportItem 是其组成部分。
 * 所有对 report items 的修改必须通过 EvaluationReport.addItem() 进行。
 */
@TableName("agent_eval_reports")
public class EvaluationReport extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String suiteId;
    private String agentId;
    private String name;
    private String status;              // "IN_PROGRESS", "COMPLETED", "PARTIAL"
    private Double overallScore;
    private Double trajectoryScore;
    private Double llmJudgeScore;
    private Integer totalCases;
    private Integer completedCases;
    private Integer passedCases;
    private String summary;
    private String configJson;
    private String userId;

    /** 报告的详细条目（聚合关系，不在表中存储） */
    @TableField(exist = false)
    private List<EvalReportItem> items = new ArrayList<>();

    public void addItem(EvalReportItem item) {
        this.items.add(item);
    }

    /** 计算聚合统计 */
    public void computeAggregates() {
        if (items.isEmpty()) return;

        this.totalCases = items.size();
        this.completedCases = (int) items.stream().filter(i -> i.getOverallScore() != null).count();
        this.passedCases = (int) items.stream().filter(i -> Boolean.TRUE.equals(i.getPassed())).count();

        this.overallScore = items.stream()
                .filter(i -> i.getOverallScore() != null)
                .mapToDouble(EvalReportItem::getOverallScore)
                .average().orElse(0.0);

        this.trajectoryScore = items.stream()
                .filter(i -> i.getTrajectoryScore() != null)
                .mapToDouble(EvalReportItem::getTrajectoryScore)
                .average().orElse(0.0);

        this.llmJudgeScore = items.stream()
                .filter(i -> i.getLlmScore() != null)
                .mapToDouble(EvalReportItem::getLlmScore)
                .average().orElse(0.0);

        this.summary = String.format("共%d个用例, 完成%d个, 通过%d个 (%.0f%%)",
                totalCases, completedCases, passedCases,
                totalCases > 0 ? 100.0 * passedCases / totalCases : 0);

        this.status = completedCases >= totalCases ? "COMPLETED" : "PARTIAL";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSuiteId() { return suiteId; }
    public void setSuiteId(String suiteId) { this.suiteId = suiteId; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }

    public Double getTrajectoryScore() { return trajectoryScore; }
    public void setTrajectoryScore(Double trajectoryScore) { this.trajectoryScore = trajectoryScore; }

    public Double getLlmJudgeScore() { return llmJudgeScore; }
    public void setLlmJudgeScore(Double llmJudgeScore) { this.llmJudgeScore = llmJudgeScore; }

    public Integer getTotalCases() { return totalCases; }
    public void setTotalCases(Integer totalCases) { this.totalCases = totalCases; }

    public Integer getCompletedCases() { return completedCases; }
    public void setCompletedCases(Integer completedCases) { this.completedCases = completedCases; }

    public Integer getPassedCases() { return passedCases; }
    public void setPassedCases(Integer passedCases) { this.passedCases = passedCases; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<EvalReportItem> getItems() { return items; }
    public void setItems(List<EvalReportItem> items) { this.items = items; }
}
