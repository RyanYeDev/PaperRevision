package com.paperrevision.domain.evaluation.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paperrevision.infrastructure.entity.BaseEntity;

/** 测试套件与测试用例的多对多关联 */
@TableName("agent_test_suite_cases")
public class TestSuiteCaseEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String suiteId;
    private String caseId;
    private Integer sortOrder;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSuiteId() { return suiteId; }
    public void setSuiteId(String suiteId) { this.suiteId = suiteId; }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
