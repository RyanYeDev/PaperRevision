package com.paperrevision.infrastructure.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Gateway服务 - 管理与MCP网关的交互
 * 生产环境对接 langchain4j-mcp，本地开发模式为stub
 */
@Service
public class MCPGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(MCPGatewayService.class);

    @Value("${mcp.gateway.base-url:http://localhost:8081}")
    private String gatewayBaseUrl;

    @Value("${mcp.gateway.api-key:}")
    private String apiKey;

    /** 获取MCP工具列表 */
    public List<String> getMcpTools(String mcpServerName) {
        logger.info("MCP工具列表查询: {} (stub模式)", mcpServerName);
        return List.of("search_references", "check_citation", "compare_text");
    }

    /** 调用MCP工具 */
    public String callMcpTool(String mcpServerName, String toolName, String arguments) {
        logger.info("MCP工具调用: {}.{}({}) (stub模式)", mcpServerName, toolName, arguments);
        return "{\"result\": \"MCP stub response for " + toolName + "\"}";
    }
}
