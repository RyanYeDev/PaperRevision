package org.xhy.domain.workflow.model;

import java.util.Map;

/** 工作流节点 */
public class WorkflowNode {

    private String id;
    private String name;
    private String type; // START, TASK, DECISION, END
    private String toolName; // 关联的工具名
    private Map<String, Object> params;
    private String nextOnSuccess;
    private String nextOnFailure;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    public String getNextOnSuccess() { return nextOnSuccess; }
    public void setNextOnSuccess(String nextOnSuccess) { this.nextOnSuccess = nextOnSuccess; }
    public String getNextOnFailure() { return nextOnFailure; }
    public void setNextOnFailure(String nextOnFailure) { this.nextOnFailure = nextOnFailure; }
}
