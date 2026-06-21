package cn.wjagent.ai.test.app;

import cn.wjagent.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AiAgentAutoConfigTest {

    @Resource
    protected ApplicationContext applicationContext;

    @Test
    public void test_agent_register() {
        AiAgentRegisterVO vo = applicationContext.getBean("100001", AiAgentRegisterVO.class);

        log.info("装配结果 appName={} agentId={} agentName={}", vo.getAppName(), vo.getAgentId(), vo.getAgentName());

        Assert.assertNotNull("Runner 不应为空", vo.getRunner());
        Assert.assertEquals("testAgent01", vo.getAppName());
        Assert.assertEquals("100001", vo.getAgentId());
    }

    @Test
    public void test_agent_pipeline() {
        AiAgentRegisterVO vo = applicationContext.getBean("100001", AiAgentRegisterVO.class);

        InMemoryRunner runner = vo.getRunner();
        Session session = runner.sessionService()
                .createSession(vo.getAppName(), "test-user")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("编写一个二分查找算法"));
        Flowable<Event> events = runner.runAsync("test-user", session.id(), userMsg);

        List<String> finalOutputs = events.filter(Event::finalResponse)
                .map(Event::stringifyContent)
                .toList()
                .blockingGet();

        Assert.assertFalse("Pipeline 应有最终输出", finalOutputs.isEmpty());
        log.info("Pipeline 最终输出:\n{}", String.join("\n---\n", finalOutputs));
    }

    @Test
    public void test_mcp_tool_time() {
        AiAgentRegisterVO vo = applicationContext.getBean("200001", AiAgentRegisterVO.class);

        log.info("MCP 装配结果 appName={} agentId={} agentName={}", vo.getAppName(), vo.getAgentId(), vo.getAgentName());
        Assert.assertNotNull("MCP Runner 不应为空", vo.getRunner());

        InMemoryRunner runner = vo.getRunner();
        Session session = runner.sessionService()
                .createSession(vo.getAppName(), "mcp-user")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("现在几点了？请用 getCurrentTime 工具查询"));
        Flowable<Event> events = runner.runAsync("mcp-user", session.id(), userMsg);

        List<String> finalOutputs = events.filter(Event::finalResponse)
                .map(Event::stringifyContent)
                .toList()
                .blockingGet();

        Assert.assertFalse("MCP 工具调用应有最终输出", finalOutputs.isEmpty());
        log.info("MCP 工具调用输出:\n{}", String.join("\n---\n", finalOutputs));
    }

    @Test
    public void test_mcp_tool_add() {
        AiAgentRegisterVO vo = applicationContext.getBean("200001", AiAgentRegisterVO.class);

        InMemoryRunner runner = vo.getRunner();
        Session session = runner.sessionService()
                .createSession(vo.getAppName(), "mcp-user")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("请用 add 工具计算 18 + 24 等于多少"));
        Flowable<Event> events = runner.runAsync("mcp-user", session.id(), userMsg);

        List<String> finalOutputs = events.filter(Event::finalResponse)
                .map(Event::stringifyContent)
                .toList()
                .blockingGet();

        Assert.assertFalse("MCP add 工具应有最终输出", finalOutputs.isEmpty());
        log.info("MCP add 工具输出:\n{}", String.join("\n---\n", finalOutputs));

        String output = String.join("", finalOutputs);
        Assert.assertTrue("输出应包含计算结果 42", output.contains("42"));
    }

}
