# change.md — 进度记录 & 修改记录

> 每次完成一条指令/一次修改，立刻在此追加记录（最新在上）。

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
