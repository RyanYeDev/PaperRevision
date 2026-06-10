package org.xhy.domain.workflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.service.ToolRegistry;
import org.xhy.domain.workflow.model.WorkflowDefinition;
import org.xhy.domain.workflow.model.WorkflowNode;

import java.util.*;

/** DAG工作流引擎 */
@Service
public class WorkflowEngine {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngine.class);

    private final ToolRegistry toolRegistry;

    public WorkflowEngine(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /** 执行工作流 */
    public Map<String, Object> execute(WorkflowDefinition workflow, Map<String, Object> input) {
        logger.info("开始执行工作流: {}", workflow.getName());

        Map<String, WorkflowNode> nodeMap = new HashMap<>();
        for (WorkflowNode node : workflow.getNodes()) {
            nodeMap.put(node.getId(), node);
        }

        // 找到起始节点
        WorkflowNode currentNode = workflow.getNodes().stream()
                .filter(n -> "START".equals(n.getType())).findFirst().orElse(null);

        Map<String, Object> context = new HashMap<>(input);
        context.put("executionLog", new ArrayList<String>());

        int maxSteps = 50;
        int steps = 0;

        while (currentNode != null && steps < maxSteps) {
            steps++;
            logger.info("执行节点: {} ({})", currentNode.getName(), currentNode.getType());

            try {
                Map<String, Object> result = executeNode(currentNode, context);
                context.putAll(result);
                currentNode = nodeMap.get(currentNode.getNextOnSuccess());
            } catch (Exception e) {
                logger.error("节点执行失败: {}", currentNode.getName(), e);
                context.put("error", e.getMessage());
                currentNode = nodeMap.get(currentNode.getNextOnFailure());
                if (currentNode == null) break;
            }
        }

        logger.info("工作流执行完成: {}, 步骤数: {}", workflow.getName(), steps);
        return context;
    }

    /** 执行单个节点 */
    private Map<String, Object> executeNode(WorkflowNode node, Map<String, Object> context) {
        switch (node.getType()) {
            case "START":
            case "END":
                return Map.of();
            case "TASK":
                return executeTask(node, context);
            default:
                return Map.of();
        }
    }

    /** 执行任务节点 */
    private Map<String, Object> executeTask(WorkflowNode node, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<>();
        // 工具调用由Agent运行时通过langchain4j执行
        if (node.getToolName() != null && toolRegistry.getTool(node.getToolName()) != null) {
            result.put("tool_" + node.getToolName(), "executed");
        }
        result.put("node_" + node.getId(), "completed");
        return result;
    }

    /** 创建论文返修工作流 */
    public WorkflowDefinition createPaperRevisionWorkflow() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("paper-revision-workflow");
        workflow.setName("论文返修工作流");
        workflow.setDescription("解析返修意见 -> RAG检索 -> 逐条分析 -> 生成修改方案 -> 执行修改");

        WorkflowNode start = createNode("start", "开始", "START", null);
        WorkflowNode parse = createNode("parse", "解析返修意见", "TASK", "read_pdf");
        WorkflowNode rag = createNode("rag", "RAG检索参考文献", "TASK", "search_references");
        WorkflowNode analyze = createNode("analyze", "逐条分析返修意见", "TASK", "search_paper");
        WorkflowNode generate = createNode("generate", "生成修改方案", "TASK", "compare_text");
        WorkflowNode execute = createNode("execute", "执行修改", "TASK", "write_revision");
        WorkflowNode check = createNode("check", "检查引用和格式", "TASK", "check_citation");
        WorkflowNode end = createNode("end", "完成", "END", null);

        start.setNextOnSuccess("parse");
        parse.setNextOnSuccess("rag");
        rag.setNextOnSuccess("analyze");
        analyze.setNextOnSuccess("generate");
        generate.setNextOnSuccess("execute");
        execute.setNextOnSuccess("check");
        check.setNextOnSuccess("end");

        workflow.setNodes(List.of(start, parse, rag, analyze, generate, execute, check, end));
        workflow.setEdges(new ArrayList<>());

        return workflow;
    }

    private WorkflowNode createNode(String id, String name, String type, String toolName) {
        WorkflowNode node = new WorkflowNode();
        node.setId(id);
        node.setName(name);
        node.setType(type);
        node.setToolName(toolName);
        return node;
    }
}
