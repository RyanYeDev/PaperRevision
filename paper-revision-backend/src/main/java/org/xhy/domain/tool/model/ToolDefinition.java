package org.xhy.domain.tool.model;

/** 工具定义模型 */
public class ToolDefinition {

    private String name;
    private String description;
    private String toolType; // BUILTIN, MCP, CUSTOM
    private String inputSchema; // JSON Schema for tool input
    private String handlerClass;

    public ToolDefinition() {}

    public ToolDefinition(String name, String description, String toolType) {
        this.name = name;
        this.description = description;
        this.toolType = toolType;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getToolType() { return toolType; }
    public void setToolType(String toolType) { this.toolType = toolType; }
    public String getInputSchema() { return inputSchema; }
    public void setInputSchema(String inputSchema) { this.inputSchema = inputSchema; }
    public String getHandlerClass() { return handlerClass; }
    public void setHandlerClass(String handlerClass) { this.handlerClass = handlerClass; }
}
