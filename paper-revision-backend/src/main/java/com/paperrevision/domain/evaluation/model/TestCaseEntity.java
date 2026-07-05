package com.paperrevision.domain.evaluation.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paperrevision.infrastructure.entity.BaseEntity;

/**
 * 测试用例实体
 * 对应 Agent 评估知识体系中的"基准测试集"概念
 * 支持从 GAIA、HotPotQA 等标准基准集导入，也可自定义创建
 */
@TableName("agent_test_cases")
public class TestCaseEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private String description;
    private String inputData;
    private String expectedOutput;
    private String groundTruth;
    private String metadataJson;
    private String sourceDataset;   // "GAIA", "HOTPOTQA", "SWE_BENCH", "CUSTOM"
    private String difficulty;      // "EASY", "MEDIUM", "HARD"
    private String userId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    public String getGroundTruth() { return groundTruth; }
    public void setGroundTruth(String groundTruth) { this.groundTruth = groundTruth; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public String getSourceDataset() { return sourceDataset; }
    public void setSourceDataset(String sourceDataset) { this.sourceDataset = sourceDataset; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
