package com.paperrevision.domain.workflow.model;

import java.util.List;
import java.util.Map;

/** 工作流定义 */
public class WorkflowDefinition {

    private String id;
    private String name;
    private String description;
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;
    private Map<String, Object> config;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<WorkflowNode> getNodes() { return nodes; }
    public void setNodes(List<WorkflowNode> nodes) { this.nodes = nodes; }
    public List<WorkflowEdge> getEdges() { return edges; }
    public void setEdges(List<WorkflowEdge> edges) { this.edges = edges; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
}
