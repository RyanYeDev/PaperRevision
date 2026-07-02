package com.paperrevision.domain.tool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.paperrevision.domain.tool.model.ToolDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 工具注册中心 */
@Service
public class ToolRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();

    public ToolRegistry() {
        registerBuiltinTools();
    }

    /** 注册内置工具 */
    private void registerBuiltinTools() {
        register(new ToolDefinition("search_references", "搜索参考文献中的相关内容", "BUILTIN"));
        register(new ToolDefinition("check_citation", "检查引用格式是否正确", "BUILTIN"));
        register(new ToolDefinition("compare_text", "对比原文和修改后的文本差异", "BUILTIN"));
        register(new ToolDefinition("search_paper", "在论文中搜索指定内容", "BUILTIN"));
        register(new ToolDefinition("format_check", "检查论文格式是否符合要求", "BUILTIN"));
        register(new ToolDefinition("grammar_check", "检查语法和拼写错误", "BUILTIN"));
        register(new ToolDefinition("read_pdf", "读取PDF文件内容", "BUILTIN"));
        register(new ToolDefinition("write_revision", "写入修改后的内容", "BUILTIN"));
        logger.info("内置工具注册完成, 共{}个工具", tools.size());
    }

    /** 注册工具 */
    public void register(ToolDefinition tool) {
        tools.put(tool.getName(), tool);
        logger.info("工具注册: {}", tool.getName());
    }

    /** 获取工具 */
    public ToolDefinition getTool(String name) {
        return tools.get(name);
    }

    /** 获取所有工具 */
    public List<ToolDefinition> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    /** 获取指定类型的工具 */
    public List<ToolDefinition> getToolsByType(String toolType) {
        return tools.values().stream()
                .filter(t -> t.getToolType().equals(toolType))
                .toList();
    }

    /** 获取工具描述列表（给LLM用的） */
    public String getToolsDescription() {
        StringBuilder sb = new StringBuilder();
        for (ToolDefinition tool : tools.values()) {
            sb.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        }
        return sb.toString();
    }
}
