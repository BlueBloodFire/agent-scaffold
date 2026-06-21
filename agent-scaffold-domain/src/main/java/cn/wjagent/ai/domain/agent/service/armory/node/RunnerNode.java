package cn.wjagent.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.wjagent.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.wjagent.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.wjagent.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.runner.InMemoryRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RunnerNode extends AbstractArmorySupport {

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParamter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {

        log.info("Ai Agent 装配操作 - RunnerNode");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParamter.getAiAgentConfigTableVO();
        String appName = aiAgentConfigTableVO.getAppName();
        String agentId = aiAgentConfigTableVO.getAgent().getAgentId();
        String agentName = aiAgentConfigTableVO.getAgent().getAgentName();
        String agentDesc = aiAgentConfigTableVO.getAgent().getAgentDesc();

        // 获取上下文对象
        SequentialAgent sequentialAgent = dynamicContext.getSequentialAgent();

        // 会话运行节点
        InMemoryRunner runner = new InMemoryRunner(sequentialAgent, appName);

        // 构建注册对象
        AiAgentRegisterVO aiAgentRegisterVO = AiAgentRegisterVO.builder()
                .agentId(agentId)
                .appName(appName)
                .agentName(agentName)
                .agentDesc(agentDesc)
                .runner(runner)
                .build();

        // 注册到Spring容器
        registerBean(agentId, AiAgentRegisterVO.class, aiAgentRegisterVO);

        return aiAgentRegisterVO;

    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
}
