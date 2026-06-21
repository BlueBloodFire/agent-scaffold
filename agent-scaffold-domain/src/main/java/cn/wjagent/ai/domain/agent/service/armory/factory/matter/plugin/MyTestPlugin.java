package cn.wjagent.ai.domain.agent.service.armory.factory.matter.plugin;

import com.google.adk.plugins.BasePlugin;
import org.springframework.stereotype.Service;

@Service("myTestPlugin")
public class MyTestPlugin extends BasePlugin {

    public MyTestPlugin(String name) {
        super(name);
    }

    public MyTestPlugin() {
        super("MyTestPlugin");
    }

    @Override
    public Maybe<Content> onUserMessageCallback(InvocationContext invocationContext, Content userMessage) {
        log.info("用户输入信息:{}", userMessage.text());
        return super.onUserMessageCallback(invocationContext, userMessage);
    }

    @Override
    public Maybe<Content> beforeAgentCallback(BaseAgent agent, CallbackContext callbackContext) {
        String agentName = agent.name();

        log.info("智能体名称:{}", agentName);

        return super.beforeAgentCallback(agent, callbackContext);
    }

    @Override
    public Maybe<LlmResponse> beforeModelCallback(CallbackContext callbackContext, LlmRequest llmRequest) {
        Optional<String> model = llmRequest.model();
        log.info("ai 模型:{}", model.orElse(""));

        return super.beforeModelCallback(callbackContext, llmRequest);
    }

}
