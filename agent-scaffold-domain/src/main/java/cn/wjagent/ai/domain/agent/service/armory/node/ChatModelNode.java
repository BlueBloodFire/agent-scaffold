package cn.wjagent.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.wjagent.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.wjagent.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.wjagent.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.wjagent.ai.domain.agent.service.armory.factory.matter.mcp.client.ToolMcpCreateService;
import cn.wjagent.ai.domain.agent.service.armory.factory.matter.mcp.client.factory.DefaultMcpClientFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class ChatModelNode extends AbstractArmorySupport {

    @Resource
    private AgentNode agentNode;

    @Resource
    private DefaultMcpClientFactory defaultMcpClientFactory;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParamter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {

        log.info("Ai Agent 装配操作 - ChatModelNode");

        OpenAiApi openAiApi = dynamicContext.getOpenAiApi();

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParamter.getAiAgentConfigTableVO();
        AiAgentConfigTableVO.Module.ChatModel chatModelConfig = aiAgentConfigTableVO.getModule().getChatModel();
        List<AiAgentConfigTableVO.Module.ChatModel.ToolMcp> toolMcpList = chatModelConfig.getToolMcpList();

        // 构建mcp服务（工厂）
        List<ToolCallback> toolCallbackList = new ArrayList<>();
        if (toolMcpList != null) {
            for (AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp : toolMcpList) {
                ToolMcpCreateService toolMcpCreateService = defaultMcpClientFactory.getToolMcpCreateService(toolMcp);
                ToolCallback[] toolCallbacks = toolMcpCreateService.buildToolCallback(toolMcp);
                toolCallbackList.addAll(List.of(toolCallbacks));
            }
        }

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModelConfig.getModel())
                        .toolCallbacks(toolCallbackList)
                        .build())
                .build();

        dynamicContext.setChatModel(chatModel);

        return router(requestParamter, dynamicContext);

    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return agentNode;
    }

    @Override
    protected void multiThread(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        super.multiThread(requestParameter, dynamicContext);
    }


}
