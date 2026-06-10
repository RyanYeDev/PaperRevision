package org.xhy.infrastructure.mcp;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/** MCP Gateway服务 - 管理与MCP网关的交互 */
@Service
public class MCPGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(MCPGatewayService.class);

    @Value("${mcp.gateway.base-url:http://localhost:8081}")
    private String gatewayBaseUrl;

    @Value("${mcp.gateway.api-key:}")
    private String apiKey;

    /** 创建MCP客户端连接 */
    public McpClient createMcpClient(String mcpServerName) {
        String sseUrl = gatewayBaseUrl + "/" + mcpServerName + "/sse";
        if (apiKey != null && !apiKey.isEmpty()) {
            sseUrl += "?api_key=" + apiKey;
        }

        HttpMcpTransport transport = HttpMcpTransport.builder()
                .sseUrl(sseUrl)
                .timeout(Duration.ofSeconds(30))
                .build();

        McpClient client = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        logger.info("MCP客户端创建成功: {}", mcpServerName);
        return client;
    }

    /** 获取MCP工具列表 */
    public List<String> getMcpTools(String mcpServerName) {
        try (McpClient client = createMcpClient(mcpServerName)) {
            return client.listTools().stream()
                    .map(t -> t.name())
                    .toList();
        } catch (Exception e) {
            logger.error("获取MCP工具列表失败: {}", mcpServerName, e);
            return List.of();
        }
    }

    /** 调用MCP工具 */
    public String callMcpTool(String mcpServerName, String toolName, String arguments) {
        try (McpClient client = createMcpClient(mcpServerName)) {
            var result = client.executeTool(toolName, arguments);
            logger.info("MCP工具调用完成: {}.{}", mcpServerName, toolName);
            return result;
        } catch (Exception e) {
            logger.error("MCP工具调用失败: {}.{}", mcpServerName, toolName, e);
            throw new RuntimeException("MCP工具调用失败: " + e.getMessage());
        }
    }
}
