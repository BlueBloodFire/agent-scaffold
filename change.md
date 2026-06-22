# change.md — 进度记录 & 修改记录

> 每次完成一条指令/一次修改，立刻在此追加记录（最新在上）。

## Skills 工具装配支持 ✅（2026-06-21）

**新增/修改文件**：
- `pom.xml`（根）：dependencyManagement 新增 `org.springaicommunity:spring-ai-agent-utils:0.4.2`
- `agent-scaffold-domain/pom.xml`：引入 `spring-ai-agent-utils` 依赖
- `AiAgentConfigTableVO.java`：`ChatModel` 内新增 `Skill` 内部类（`resource` / `directory` 两个字段）和 `skillList` 字段
- `ChatModelNode.java`：实现 `ResourceLoaderAware`，在 MCP 装配之后追加 skills 装配逻辑

**使用方式**：在 agent 的 `chat-model` 配置下加 `skill-list`：

```yaml
chat-model:
  model: deepseek-v4-flash
  tool-mcp-list: []
  skill-list:
    - resource: classpath:skills/my-skill.md   # classpath 资源加载
    - directory: /opt/agent/skills/            # 绝对路径目录扫描
```

所有 skill 合并为一个 `SkillsTool` ToolCallback 注册到 ChatModel，Agent 运行时可通过该工具按名称调用具体 skill 内容。

## Nginx 静态页面实现 ✅（2026-06-21）

**新增/修改文件**：`docs/dev-ops/nginx/html/login.html`、`docs/dev-ops/nginx/html/index.html`

按时序图完整实现两个静态页面：

**login.html**
- admin/admin 演示登录校验
- 登录成功写 cookie `ai_agent_login=admin`（有效期 1 天），跳转 index.html
- 回车触发登录

**index.html**
- 初始化时校验 cookie `ai_agent_login`，未登录跳回 login.html
- 读取 `js/config.js` 中 `window.APP_CONFIG.API_BASE`（默认 `http://127.0.0.1:8091`）
- 侧边栏调用 `GET /api/v1/query_ai_agent_config_list` 加载智能体列表
- 点击智能体后可新建会话（`POST /api/v1/create_session`）或复用 currentSessionId
- 输入框 Enter 发送，Shift+Enter 换行；调用 `POST /api/v1/chat`
- 服务不可达/CORS 失败时弹窗提示，显示当前 API_BASE，提供"重试"按钮

## Session 失效自动重建修复 ✅（2026-06-21）

**根因**：ADK 使用 `InMemoryRunner`，会话仅存内存。服务重启后内存清空，但前端 `currentSessionId` 变量保留旧值，跳过 `create_session` 直接调 `/chat`，ADK 抛 `Session not found: <id> for user <userId>`，前端显示"服务不可达"。

**修复文件**
- `ChatService.java`（`handleMessage` + `handleMessageStream`）：`runner.runAsync` 外包 try-catch，捕获 `IllegalArgumentException`（Session not found），自动重建 session 并更新 `userSessions` 缓存后重试，对调用方透明
- `index.html`（`doChat`）：`/chat` 返回非 0000 时将 `currentSessionId = null`，下次 `sendMessage` 重走 `ensureSession()` 创建新 session，不会再复用失效 sessionId

**验证**：`mvn compile` BUILD SUCCESS（全 6 模块）

## Nginx 静态页面补全 ✅（2026-06-21）

**新增文件**
- `docs/dev-ops/nginx/html/js/config.js`：`window.APP_CONFIG.API_BASE` 配置，默认 `http://127.0.0.1:8091`；`index.html` 通过 `<script src="js/config.js">` 引用，文件不存在会导致 404 中断初始化流程
- `docs/dev-ops/nginx/nginx.conf`：Nginx 配置，`root html`，访问 `/` 302 跳转 `login.html`；`/api/` 反向代理到 `host.docker.internal:8091`（Spring Boot）

**完整登录流**
1. 浏览器访问 `http://localhost/` → Nginx 302 → `login.html`
2. 输入 admin/admin → 写 cookie `ai_agent_login=admin`（1天）→ 跳转 `index.html`
3. `index.html` 初始化时读 cookie，未登录跳回 `login.html`；登录后加载智能体列表 → 选择智能体 → 对话

## 全项目编译修复 ✅（2026-06-21）

**修复文件**：`agent-scaffold-api/pom.xml`
- 新增 `spring-webmvc` 依赖：`IAgentService` 接口引用 `ResponseBodyEmitter`（包路径 `org.springframework.web.servlet.mvc.method.annotation`），但 api 模块原无 spring-webmvc，导致编译失败；版本由 `spring-boot-starter-parent` 统一管理，无需指定

**验证**：`mvn compile` 全 6 模块 BUILD SUCCESS

## AgentServiceController 修复 + HTTP 接口测试 ✅（2026-06-21）

**修复文件**：`trigger/http/AgentServiceController.java`

5 处错误全部修复：
1. **补全所有 import**：`IAgentService / IChatService / dto.* / Response / AiAgentConfigTableVO / ResponseCode / AppException / @Resource / @Slf4j / @RestController 等`
2. **补声明 `implements IAgentService`**：类声明中漏写，导致所有 `@Override` 报错
3. **`createSession` GET → POST**：`@RequestBody` 不能配合 GET 请求，改为 POST
4. **修复 `createSession` catch 日志**：copy-paste 遗留的"查询智能体配置列表异常"改为"创建会话异常"
5. **`chatStream` 补 sessionId 空判断**：与 `chat` 接口一致，sessionId 为空时自动创建会话，避免 `runner.runAsync` NPE

**新增文件**：`app/src/test/java/cn/wjagent/ai/test/app/AgentServiceControllerTest.java`

5 个测试用例，使用 `@SpringBootTest + @AutoConfigureMockMvc`：
- `test_queryAiAgentConfigList`：GET 接口，不依赖 AI，读 YAML 配置即可
- `test_createSession`：POST 创建会话，断言返回成功码 + sessionId
- `test_createSession_agentNotFound`：传不存在 agentId，断言返回 E0001
- `test_chat`：POST 阻塞式对话（需 AI 凭证）
- `test_chatStream`：POST 流式对话（需 AI 凭证）
- `test_chat_withExistingSession`：先创建 session，再用 sessionId 发起对话

## ChatService 对话层修复 ✅（2026-06-21）

**新增文件**（用户添加，本次修复编译错误）
- `IChatService`（`domain/agent/service/`）：对话服务接口，定义 `queryAiAgentConfigList / createSession / handleMessage（3 重载）/ handleMessageStream`
- `ChatService`（`domain/agent/service/chat/`）：接口实现，用 `ConcurrentHashMap<userId, sessionId>` 缓存会话，支持文本/文件/InlineData 多模态消息，委托 ADK `InMemoryRunner` 执行
- `ChatCommandEntity`（`domain/agent/model/entity/`）：多模态消息实体，含 `Content.Text / Content.File / Content.InlineData` 三个内部类，提供 `buildSessionCommand / buildChatCommand` 辅助方法

**修复文件**
- `ChatService`：补全所有缺失 import（jakarta.annotation.Resource、genai Content/Part、adk Event/InMemoryRunner/Session、rxjava Flowable、java.util.\*）；修正 `AiAgentAutoConfigProperties` 包路径为 `model/valobj/properties/`
- `DefaultArmoryFactory`：注入 `ApplicationContext`，新增 `getAiAgentRegisterVO(String agentId)` — 按 agentId 从 Spring 容器获取 `RunnerNode` 注册的 `AiAgentRegisterVO` bean，未找到时返回 `null`
- `ResponseCode`：新增 `E0001("E0001", "Agent不存在")` 枚举值

**验证**：`mvn compile` BUILD SUCCESS

## 插件增强 + Prometheus 监控插件 ✅（2026-06-21）

**新增文件**
- `PrometheusPlugin`（`factory/matter/plugin/`）：基于 Micrometer 的 Prometheus 监控插件，拦截 `beforeAgentCallback / afterAgentCallback / beforeModelCallback / afterModelCallback`，记录：
  - `agent.invocation.count{agent_name}` — Agent 调用计数
  - `agent.invocation.duration{agent_name}` — Agent 执行耗时（Timer）
  - `model.call.count{model}` — 模型调用计数
  - `model.call.duration` — 模型调用耗时（Timer）
  - 用 `ConcurrentHashMap<threadId, Timer.Sample>` 保证并行 Agent 各自独立计时

**修复文件**
- `MyTestPlugin`：补全所有缺失 import（`InvocationContext/CallbackContext/BaseAgent/LlmRequest/LlmResponse/Content/Maybe`）；加 `@Slf4j`；修正 `beforeModelCallback` 参数为 ADK 0.4.0 实际签名 `(CallbackContext, LlmRequest)`（非 `LlmRequest.Builder`，`Builder` 是 1.2.0 API）；删除多余构造器

**pom 变更**
- `agent-scaffold-domain/pom.xml`：添加 `micrometer-core`（`PrometheusPlugin` 编译期需要 `MeterRegistry`）
- `agent-scaffold-app/pom.xml`：添加 `spring-boot-starter-actuator` + `micrometer-registry-prometheus`

**配置变更**
- `application-dev.yml`：新增 `management.endpoints.web.exposure.include: prometheus,health,info` 及应用标签
- `test-agent.yml`：runner 增加 `plugin-name-list: [myLogPlugin, prometheusPlugin]`

**测试**
- `test_plugin_log`：验证插件链路正常挂载，日志中可见 Agent/Model 拦截日志
- `test_plugin_prometheus_metrics`：运行后从 `MeterRegistry` 断言 `agent.invocation.count > 0`、`agent.invocation.duration`、`model.call.count > 0`、`model.call.duration` 均存在

**关键坑**：ADK `0.4.0` 的 `beforeModelCallback` 参数是 `LlmRequest`，`1.2.0` 才改为 `LlmRequest.Builder`；Maven 本地仓库实际在 `E:\apache-maven-3.8.8\repository`，非默认 `.m2`

## MCP Local 工具调用测试用例 ✅（2026-06-21）

- `AiAgentAutoConfigTest` 新增两个 MCP 测试方法：
  - `test_mcp_tool_time`：让 LLM 调用 `getCurrentTime` 工具，断言有输出
  - `test_mcp_tool_add`：让 LLM 调用 `add(18,24)`，断言输出含 `42`
- 两个用例均使用 agent-id `200001`（`mcp-test-agent.yml`）

## MCP 增强功能 & 枚举重构 ✅（2026-06-21）

**新增文件**
- `McpTypeEnum`（`model/valobj/enums/`）：LOCAL/SSE/STDIO 三值，每值持有 `beanName`；`fromToolMcp()` 静态方法替代 if-else 判断
- `mcp-test-agent.yml`（`resources/agent/`）：agent-id=200001，`tool-mcp-list` 配置 Local MCP `myTestMcpService`
- `application-dev.yml`：`spring.config.import` 新增 `mcp-test-agent.yml`

**修改文件**
- `AiAgentConfigTableVO.ToolMcp`：补充缺失的 `LocalParameters` 内部类（`name` 字段）
- `DefaultMcpClientFactory`：删除三个 `@Resource` 字段 + if-else，改用 `McpTypeEnum` + `ApplicationContextAware` 按 beanName 懒获取服务
- `LocalToolMcpCreateService`：修复 `org.jvnet.hk2` 错误 import → Spring `@Service`；补全所有缺失 import；改用 `MethodToolCallbackProvider.builder().toolObjects()` 包装本地 Bean；加 `@Slf4j`
- `SSEToolMcpCreateService`：加 `@Slf4j`；显式 `@Service("sseToolMcpCreateService")`（避免双大写前缀导致的 bean 名错误）
- `StdioToolMcpCreateService`：加 `@Slf4j`；显式 `@Service("stdioToolMcpCreateService")`
- `ChatModelNode`：补 `ToolCallback`、`ToolMcpCreateService` import；加 `toolMcpList != null` 判空；删除无用的 `createMcpSyncClient` 私有方法及相关 MCP/Jackson import
- `MyTestMcpService`：从空类改为 `@Component("myTestMcpService")`，含 `@Tool` 注解的 `getCurrentTime()` 和 `add()` 两个工具方法

**编译问题**：PowerShell `Out-File` 写出 UTF-16 BOM，Java 编译器报"非法字符 \\ufeff"；改用 `[System.Text.UTF8Encoding]::new($false)` 写入后解决，`mvn compile` BUILD SUCCESS

## Agent Workflow 节点流转重构 ✅（2026-06-21）

- 用户重写了 `AgentWorkflowNode`、`LoopAgentNode`、`ParallelAgentNode`、`SequentialAgentNode`
- 流转方式从 `agentWorkflows.remove(0)` 列表变更改为 `AtomicInteger currentStepIndex` 索引递进
- `DefaultArmoryFactory.DynamicContext` 新增 `currentStepIndex`（AtomicInteger）、`currentAgentWorkflow`、`addCurrentStepIndex()`
- 三个 workflow 节点的 `get()` 均返回 `getBean("agentWorkflowNode")` 实现循环回路（取代 `@Resource` 兄弟节点注入，解决循环依赖）
- 修复 `AgentTypeEnum`：从 `class` 改为 `enum`，删除 `@NoArgsConstructor`（Lombok 与 enum 冲突）
- `test-agent.yml`：新增 sequential / parallel / loop / root 四条 agent-workflow 配置，覆盖三种节点类型

## Agent 配置化装配节点链 ✅（2026-06-21）

**提交**：`节点装配`（推送至 GitHub master）

**责任链节点**（`domain/service/armory/node/`）：
- `RootNode → AiApiNode → ChatModelNode → AgentNode → AgentWorkflowNode → [SequentialAgentNode | ParallelAgentNode | LoopAgentNode] → RunnerNode`
- 每个节点实现 `AbstractArmorySupport`（extends `AbstractMultiThreadStrategyRouter`）
- `DefaultArmoryFactory.DynamicContext` 贯穿全链，携带 `OpenAiApi`、`ChatModel`、`agentGroup`、`runner` 等状态

**MCP 骨架**（`domain/service/armory/mcp/`）：
- `ToolMcpCreateService` 接口 + SSE/Stdio/Local 三个实现（此阶段为骨架，完整实现见后续条目）
- `DefaultMcpClientFactory` 工厂

**配置**：
- `AiAgentConfigTableVO`：完整 VO 含 AiApi/ChatModel/Agent/AgentWorkflow/Runner 五个内部类
- `test-agent.yml`：DeepSeek 配置（base-url/model/api-key），agent-id=100001

**测试**：
- `AiAgentAutoConfigTest.test_agent_register()`：验证 Bean 装配 + Runner 非空
- `AiAgentAutoConfigTest.test_agent_pipeline()`：跑完整 pipeline，断言有最终输出

## Agent 测试 mock 改为有状态 ✅（2026-06-12）

- 三个 AgentTest（springai/langchain4j/adk）的 OpsTools mock 原来无状态：mysql 永远返回 stopped，导致 Agent 重启后复查仍 stopped，反复 重启→查询 循环 3 次才放弃（行为正确但日志易误读为出错）
- 改为有状态 Map：`restartService` 后 `getServiceStatus` 返回 running，Agent 一次重启即收敛
- `mvn test-compile` BUILD SUCCESS

## 清理脚手架来源痕迹 + 重写初始提交 ✅（2026-06-12）

- 删除/改写所有 小傅哥/xiaofuge/xfg/bugstack 引用：README 重写、Dockerfile MAINTAINER 改 Jin、build.sh 注释、两处 package-info 链接、docker-compose 注释、数据库名 `xfg_frame_archetype` → `agent_scaffold`（三个 application-*.yml）
- 删除文件：`docker-compose-environment-aliyun.yml`（xfg-studio 镜像源）、`docs/dev-ops/mysql/sql/xfg-frame-archetype.sql`（脚手架演示数据）
- `git commit --amend` 重写根提交（85ba903）：作者仅 Jin，去掉 Co-Authored-By 落款，force push
- TestApiConfig 提交占位符 key；真实 key 仅保留在本地工作区（未提交状态，**勿 git add 此文件的 key 改动**）

## 项目初始化 ✅（2026-06-12）

**工程基础**：DDD 六模块脚手架（api/app/domain/infrastructure/trigger/types，包根 `cn.wjagent.ai`，Spring Boot 3.4.3 / JDK 17）。

**依赖引入**（根 pom dependencyManagement + app 模块依赖）：
- Spring AI BOM `1.1.5`（spring-ai-openai、spring-ai-client-chat）
- LangChain4j BOM `1.4.0`（langchain4j、langchain4j-open-ai）
- Google ADK `1.2.0`（google-adk、google-adk-spring-ai 桥接）

**AI 测试**（`agent-scaffold-app/src/test/java/cn/wjagent/ai/test/`，均为 main 方法可独立运行）：
- `TestApiConfig` — baseUrl/apiKey/model 统一配置（apikey 需自行替换）
- springai/ langchain4j/ adk/ 三包，每包 Model / Tool / Agent 三个测试，共 9 个
- Agent 测试统一场景：运维 Agent 查服务状态→发现停止→自动重启→两轮对话验证记忆

**文档体系**：新增 CLAUDE.md（本地，不提交）+ build.md / framework.md / function.md / change.md。

**验证**：`mvn test-compile -pl agent-scaffold-app -am` BUILD SUCCESS。

**Git**：.gitignore 追加 CLAUDE.md；初始提交并推送 GitHub。
