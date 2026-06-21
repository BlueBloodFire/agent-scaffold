package cn.wjagent.ai.domain.agent.service.armory.factory.matter.mcp.client.impl;

import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.service.armory.factory.matter.mcp.client.ToolMcpCreateService;
import com.networknt.schema.utils.StringUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service("sseToolMcpCreateService")
public class SSEToolMcpCreateService implements ToolMcpCreateService {

    @Override
    public ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) {
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();

        String originalBaseUri = sseConfig.getBaseUri();
        String baseUri;
        String sseEndpoint;

        int queryParamStartIndex = originalBaseUri.indexOf("sse");
        if (queryParamStartIndex != -1) {
            baseUri = originalBaseUri.substring(0, queryParamStartIndex - 1);
            sseEndpoint = originalBaseUri.substring(queryParamStartIndex - 1);
        } else {
            baseUri = originalBaseUri;
            sseEndpoint = sseConfig.getSseEndpoint();
        }

        sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;

        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                .builder(baseUri) // 使用截取后的 baseUri
                .sseEndpoint(sseEndpoint) // 使用截取或默认的 sseEndpoint
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofMinutes(sseConfig.getRequestTimeout())).build();
        var init_sse = mcpSyncClient.initialize();

        log.info("Tool SSE MCP Initialized {}", init_sse);

        return SyncMcpToolCallbackProvider.builder().mcpClients(mcpSyncClient).build()
                .getToolCallbacks();
    }
}
