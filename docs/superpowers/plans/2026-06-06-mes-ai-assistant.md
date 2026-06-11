# MES AI Assistant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an authenticated AI assistant that answers production-related natural-language questions from MES project knowledge and authorized production query data, without displaying code, SQL, internal API paths, configuration, secrets, or unauthorized tenant data.

**Architecture:** The assistant is a read-only production intelligence layer. Frontend chat UI sends questions to a backend AI endpoint; backend classifies intent, retrieves project/business context, invokes only whitelisted query tools under the current JWT tenant and permissions, calls a configurable model provider, and sanitizes the final answer before returning it.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Pinia, Axios, Spring Boot 3.2.x, Java 17, MyBatis Plus, Spring Security, JWT, Redis/Sentinel rate limiting, existing audit log framework, configurable external or private LLM provider.

---

## Non-Negotiable Boundaries

- The assistant is read-only. It must not create, update, delete, approve, release, dispatch, report, close, or retry production records.
- The assistant inherits the current user's JWT authentication, tenant context, and existing backend permission checks.
- The assistant cannot access cross-tenant data unless the existing platform-super-admin context explicitly allows it, and even then it must not disclose another tenant's data in ordinary chat.
- The assistant must not show backend code, frontend code, SQL, API routes, system configuration, environment variables, secrets, stack traces, or internal implementation details.
- The assistant answers production-domain questions only: work orders, production jobs, dispatch, quality, abnormal contact, material, APS, work status, and MES process/business guidance.
- When evidence is missing, the assistant must say the system has no matching evidence instead of inventing facts.
- Model API keys and provider configuration stay backend-only.

## File Structure Map

### Backend Files To Create

- `mes-backend/mes-ai/pom.xml`: new AI assistant business module.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/controller/AiAssistantController.java`: authenticated chat endpoint.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/domain/dto/AiChatRequest.java`: request fields for question, optional page context, and conversation id.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/domain/vo/AiChatResponse.java`: sanitized answer, related module labels, evidence summary, suggested navigation targets, and refusal reason.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/domain/model/AiIntent.java`: production intent categories.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/domain/model/AiToolResult.java`: normalized read-only tool result.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/config/AiAssistantProperties.java`: provider, timeout, token, rate, and feature switch settings.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiAssistantService.java`: orchestration interface.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/AiAssistantServiceImpl.java`: intent, retrieval, model call, guardrail, and response orchestration.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiModelClient.java`: provider-neutral model client interface.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/ConfigurableAiModelClient.java`: external/private provider adapter with backend-only credentials.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/DisabledAiModelClient.java`: safe fallback when AI is disabled or not configured.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiKnowledgeService.java`: project/business knowledge retrieval interface.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/ProjectKnowledgeServiceImpl.java`: curated MES module knowledge, generated from project docs and safe business metadata.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiToolRegistry.java`: registry of read-only production query tools.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/ProductionQueryToolRegistry.java`: tool implementations backed by existing business services.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiGuardrailService.java`: input and output boundary checks.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/AiGuardrailServiceImpl.java`: code, SQL, secret, route, stack trace, and off-domain detection.
- `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiAuditService.java`: assistant audit event facade.
- `mes-backend/mes-ai/src/test/java/com/mes/ai/service/AiGuardrailServiceTest.java`: boundary tests.
- `mes-backend/mes-ai/src/test/java/com/mes/ai/service/AiAssistantServiceTest.java`: orchestration tests.
- `mes-backend/mes-ai/src/test/java/com/mes/ai/service/ProductionQueryToolRegistryTest.java`: tool allowlist and authorization tests.

### Backend Files To Modify

- `mes-backend/pom.xml`: add `mes-ai` to Maven modules and dependency management.
- `mes-backend/mes-admin/pom.xml`: include `mes-ai` for monolith deployment.
- `mes-backend/mes-query-service/pom.xml`: include `mes-ai` for query-service deployment, unless the team chooses a dedicated AI microservice.
- `mes-backend/mes-gateway/src/main/resources/application.yml`: add `/api/ai/**` routing only if microservice mode routes through gateway.
- `mes-backend/mes-admin/src/main/resources/application.yml`: add AI assistant feature switch and provider defaults with no secrets.
- `mes-backend/mes-admin/src/main/resources/application-prod.yml`: keep AI disabled unless provider credentials and explicit enable flag are present.
- `sql/V2.23__ai_assistant_permission.sql`: add AI assistant menu or button permission template with `ai:assistant:chat`.

### Frontend Files To Create

- `mes-frontend/src/api/ai/assistant.ts`: AI chat API wrapper.
- `mes-frontend/src/types/ai.ts`: request, response, evidence, suggestion, and refusal types.
- `mes-frontend/src/components/AiAssistant/AiAssistantDrawer.vue`: chat drawer UI.
- `mes-frontend/src/components/AiAssistant/AiAssistantMessage.vue`: answer and refusal rendering.
- `mes-frontend/src/components/AiAssistant/AiAssistantSuggestions.vue`: production question starter prompts and navigation suggestions.
- `mes-frontend/tests/e2e/ai-assistant.spec.ts`: chat drawer, refusal, and authorized query flow tests.

### Frontend Files To Modify

- `mes-frontend/src/layout/MainLayout.vue`: add assistant trigger in the top toolbar for authenticated users with permission.
- `mes-frontend/src/layout/menuConfig.ts`: add optional AI assistant permission metadata if menu-based visibility is preferred.
- `mes-frontend/src/stores/permission.ts`: expose permission helper if it is not already available to layout components.
- `mes-frontend/src/locales/zh-CN.ts`: add Chinese labels for assistant UI.
- `mes-frontend/src/locales/en-US.ts`: add English labels for assistant UI if current i18n coverage requires it.

## Task 1: Add Backend AI Module Skeleton

**Files:**
- Create: `mes-backend/mes-ai/pom.xml`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/controller/AiAssistantController.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/domain/dto/AiChatRequest.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/domain/vo/AiChatResponse.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiAssistantService.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/AiAssistantServiceImpl.java`
- Modify: `mes-backend/pom.xml`
- Modify: `mes-backend/mes-admin/pom.xml`
- Modify: `mes-backend/mes-query-service/pom.xml`

- [ ] Define the AI module as a business module that depends on `mes-framework` and only the business modules needed for read-only tools.
- [ ] Register `mes-ai` in the backend parent build.
- [ ] Register `mes-ai` in monolith deployment through `mes-admin`.
- [ ] Register `mes-ai` in query-service deployment for microservice mode.
- [ ] Add the authenticated assistant endpoint under the AI domain with a single chat operation.
- [ ] Return the existing `R` response wrapper so frontend request handling stays consistent.
- [ ] Protect the endpoint with `ai:assistant:chat`.
- [ ] Verify the backend build recognizes the new module.

## Task 2: Add Feature Configuration And Safe Provider Defaults

**Files:**
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/config/AiAssistantProperties.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiModelClient.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/ConfigurableAiModelClient.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/DisabledAiModelClient.java`
- Modify: `mes-backend/mes-admin/src/main/resources/application.yml`
- Modify: `mes-backend/mes-admin/src/main/resources/application-prod.yml`

- [ ] Add an `enabled` switch that defaults to disabled in production.
- [ ] Add provider fields for base URL, model name, timeout, and max answer length.
- [ ] Read provider credentials only from backend environment variables or secret-managed runtime configuration.
- [ ] Ensure the disabled provider returns a safe business message instead of failing with technical details.
- [ ] Ensure no provider secrets are logged, audited, returned to frontend, or stored in chat history.
- [ ] Verify startup succeeds when AI is disabled.
- [ ] Verify startup fails clearly when AI is enabled but required provider settings are missing.

## Task 3: Define Intent And Domain Scope

**Files:**
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/domain/model/AiIntent.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiKnowledgeService.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/ProjectKnowledgeServiceImpl.java`
- Modify: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/AiAssistantServiceImpl.java`

- [ ] Define supported intent categories: production query, work order status, dispatch status, quality status, abnormal issue, material status, APS status, process guidance, and general MES consultation.
- [ ] Define unsupported categories: source code, SQL, system configuration, credentials, cross-tenant data, personal data not required for production work, and non-MES general chat.
- [ ] Build curated project knowledge from safe business descriptions in `README.md` and `架构设计与功能点说明文档.md`.
- [ ] Make project knowledge business-facing: module names, workflows, statuses, and safe operational explanations only.
- [ ] Exclude code, internal paths, database DDL, API route details, and implementation snippets from assistant-visible knowledge.
- [ ] Verify a supported production consultation question receives project-grounded context.
- [ ] Verify an unsupported code/configuration question is refused before model invocation.

## Task 4: Build Read-Only Production Query Tool Registry

**Files:**
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/domain/model/AiToolResult.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiToolRegistry.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/ProductionQueryToolRegistry.java`
- Modify: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/AiAssistantServiceImpl.java`

- [ ] Add a registry of explicit read-only tools.
- [ ] Connect production work questions to the existing production work query service.
- [ ] Connect inspection questions to the existing inspection work query service.
- [ ] Connect work status questions to the existing work status query service.
- [ ] Connect work order questions to the existing work order query service.
- [ ] Connect dispatch questions to the existing dispatch task query service.
- [ ] Connect quality questions to existing work-start-check, order-start-check, shift-handover, and recheck services.
- [ ] Connect abnormal questions to the abnormal contact service.
- [ ] Connect material questions to the material inventory and material movement services.
- [ ] Connect APS questions to APS sync log and status services.
- [ ] Normalize tool output into compact business summaries with record counts, key statuses, time ranges, and related module labels.
- [ ] Limit record counts per question to prevent leaking excessive production data.
- [ ] Ensure every tool call runs under the current tenant context and existing backend permissions.
- [ ] Verify no free-form SQL, raw table access, or dynamic repository access is introduced.

## Task 5: Add Guardrails For Input, Retrieval, And Output

**Files:**
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiGuardrailService.java`
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/AiGuardrailServiceImpl.java`
- Create: `mes-backend/mes-ai/src/test/java/com/mes/ai/service/AiGuardrailServiceTest.java`
- Modify: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/AiAssistantServiceImpl.java`

- [ ] Reject prompts asking for source code, SQL, API routes, system configuration, credentials, stack traces, exploit steps, or hidden policies.
- [ ] Reject prompts asking to bypass permissions, query another tenant, or reveal data the current user cannot access.
- [ ] Reject prompts asking the assistant to perform write operations.
- [ ] Sanitize model output before returning it to frontend.
- [ ] Remove code blocks, SQL-like statements, API route strings, internal file paths, secrets, and stack trace patterns from final answers.
- [ ] Replace removed technical content with a short business-facing refusal or safe explanation.
- [ ] Add unit tests for direct requests, indirect prompt injection, mixed production/code questions, cross-tenant questions, and model output that violates the no-code boundary.
- [ ] Verify the guardrail runs both before and after model invocation.

## Task 6: Add Audit, Rate Limit, And Observability

**Files:**
- Create: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/AiAuditService.java`
- Modify: `mes-backend/mes-ai/src/main/java/com/mes/ai/controller/AiAssistantController.java`
- Modify: `mes-backend/mes-ai/src/main/java/com/mes/ai/service/impl/AiAssistantServiceImpl.java`

- [ ] Add tenant-aware rate limiting to the assistant endpoint using the project's existing rate-limit approach.
- [ ] Record audit events for successful answer, refused answer, model failure, tool failure, and guardrail violation.
- [ ] Store only minimal audit fields: tenant, user, time, question summary, intent, tools used, result status, and refusal reason.
- [ ] Do not store full sensitive answer payloads in audit logs.
- [ ] Add structured logs with trace id, tenant id, user id, intent, provider status, and duration.
- [ ] Verify rate limit returns a business-safe message.
- [ ] Verify model failure returns a business-safe message without exposing stack traces.

## Task 7: Add AI Assistant Permission Migration

**Files:**
- Create: `sql/V2.23__ai_assistant_permission.sql`
- Modify: `mes-frontend/src/layout/menuConfig.ts`

- [ ] Add a platform template permission `ai:assistant:chat`.
- [ ] Add the assistant permission to administrator template roles.
- [ ] Keep the assistant hidden for users without permission.
- [ ] Decide whether the assistant is a top-level menu item or a toolbar-only feature; the preferred first version is toolbar-only with permission control.
- [ ] Verify new tenants receive the AI assistant permission template through existing menu cloning.
- [ ] Verify users without the permission cannot call the backend endpoint even if they manually send a request.

## Task 8: Build Frontend Chat Drawer

**Files:**
- Create: `mes-frontend/src/api/ai/assistant.ts`
- Create: `mes-frontend/src/types/ai.ts`
- Create: `mes-frontend/src/components/AiAssistant/AiAssistantDrawer.vue`
- Create: `mes-frontend/src/components/AiAssistant/AiAssistantMessage.vue`
- Create: `mes-frontend/src/components/AiAssistant/AiAssistantSuggestions.vue`
- Modify: `mes-frontend/src/layout/MainLayout.vue`
- Modify: `mes-frontend/src/stores/permission.ts`
- Modify: `mes-frontend/src/locales/zh-CN.ts`
- Modify: `mes-frontend/src/locales/en-US.ts`

- [ ] Add an assistant icon button to the top toolbar for permitted users.
- [ ] Use an Element Plus drawer so the assistant is available across production pages.
- [ ] Add starter prompts for common production questions.
- [ ] Show answer, related modules, evidence summary, refusal reason, and suggested navigation.
- [ ] Show a clear disabled state when backend AI is not configured.
- [ ] Show a clear refusal state when a question is outside scope or violates boundaries.
- [ ] Do not render Markdown code blocks or technical snippets from model output.
- [ ] Keep message layout compact and production-console friendly.
- [ ] Ensure text fits on desktop and mobile widths without overlap.
- [ ] Verify keyboard accessibility for opening, asking, clearing, and closing the drawer.

## Task 9: Add Frontend And Backend Tests

**Files:**
- Create: `mes-backend/mes-ai/src/test/java/com/mes/ai/service/AiAssistantServiceTest.java`
- Create: `mes-backend/mes-ai/src/test/java/com/mes/ai/service/ProductionQueryToolRegistryTest.java`
- Create: `mes-frontend/tests/e2e/ai-assistant.spec.ts`

- [ ] Test production consultation answers use project knowledge and stay business-facing.
- [ ] Test production query answers call only whitelisted tools.
- [ ] Test users without `ai:assistant:chat` cannot use the endpoint.
- [ ] Test no-code refusal for requests asking for code, SQL, API route, configuration, or stack trace.
- [ ] Test model output sanitizer removes forbidden technical content before frontend receives it.
- [ ] Test AI-disabled fallback message.
- [ ] Test assistant drawer opens only for permitted users.
- [ ] Test assistant drawer displays refusal and safe answer states.
- [ ] Test related navigation suggestions appear for supported production modules.

## Task 10: Verify Build, Runtime, And Acceptance Criteria

**Files:**
- Modify only files changed by the implementation tasks above.

- [ ] Run backend tests for the AI module.
- [ ] Run backend tests for affected query, workorder, dispatch, quality, abnormal, material, and APS modules when their services are wired into tools.
- [ ] Run frontend type check and production build.
- [ ] Run the AI assistant Playwright test.
- [ ] Start the application with AI disabled and verify safe fallback.
- [ ] Start the application with a configured provider in a non-production environment and verify a production consultation answer.
- [ ] Verify a natural-language production query returns a business summary and never exposes code or SQL.
- [ ] Verify a no-permission user cannot see the toolbar button and cannot call the endpoint.
- [ ] Verify a cross-tenant query is refused.
- [ ] Verify an instruction to reveal code, SQL, configuration, or internal API details is refused.

## Acceptance Checklist

- [ ] Users can ask natural-language production questions from the MES UI.
- [ ] The assistant answers based on MES project modules and authorized production data.
- [ ] The assistant does not display code, SQL, internal routes, configuration, secrets, stack traces, or implementation details.
- [ ] The assistant cannot perform write actions.
- [ ] The assistant honors JWT authentication, role permissions, and tenant isolation.
- [ ] The assistant has a safe disabled-provider fallback.
- [ ] The assistant has audit and rate-limit coverage.
- [ ] The assistant has backend guardrail tests and frontend e2e coverage.

## Self-Review

- Spec coverage: The plan covers AI intelligent answering, natural-language production querying, production consultation, project-grounded answers, no-code output, permission boundaries, tenant boundaries, frontend entry, backend orchestration, tests, and rollout.
- Placeholder scan: No task uses unresolved placeholders.
- Type consistency: Request, response, intent, tool result, model client, knowledge service, tool registry, guardrail service, and audit service names are consistent across tasks.
- Scope check: This is one cohesive feature. Optional future expansion into a dedicated AI microservice can be done later, but the first implementation stays bounded to a backend AI module plus frontend drawer.
