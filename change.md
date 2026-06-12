# change.md — 进度记录 & 修改记录

> 每次完成一条指令/一次修改，立刻在此追加记录（最新在上）。

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
