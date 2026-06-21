# change.md — 进度记录 & 修改记录

> 每次完成一条指令/一次修改，立刻在此追加记录（最新在上）。

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
