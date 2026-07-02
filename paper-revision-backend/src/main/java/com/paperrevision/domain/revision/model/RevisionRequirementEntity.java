package com.paperrevision.domain.revision.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paperrevision.infrastructure.entity.BaseEntity;

/** 返修需求实体 */
@TableName("revision_requirements")
public class RevisionRequirementEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String paperId;
    private String content;
    private String requirementType; // REVIEWER_COMMENT, FORMAT_REQUIREMENT, CONTENT_REVISION, CITATION_CHECK
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private String userId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPaperId() { return paperId; }
    public void setPaperId(String paperId) { this.paperId = paperId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getRequirementType() { return requirementType; }
    public void setRequirementType(String requirementType) { this.requirementType = requirementType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
