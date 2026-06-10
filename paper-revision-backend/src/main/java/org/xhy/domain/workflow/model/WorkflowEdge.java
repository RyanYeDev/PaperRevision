package org.xhy.domain.workflow.model;

/** 工作流边 */
public class WorkflowEdge {

    private String id;
    private String sourceNodeId;
    private String targetNodeId;
    private String condition; // onSuccess, onFailure, always

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
}
