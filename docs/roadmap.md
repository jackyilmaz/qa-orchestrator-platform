# QA Orchestrator Platform — Roadmap

## Phase 1 — Foundation ✅ Complete
- Spring Boot backend, Jira integration, pipeline, Render deployment, Copilot Studio, API contract

## Phase 2 — LLM Intelligence ✅ Complete
- Groq LLM integration, all 5 stages LLM-powered, bug report generation, retry logic

## Phase 3 — Observability & Reliability ✅ Complete
- Structured logging, health endpoint, error handling, Groq retry, Render sleep prevention

## Phase 4 — Hardening & Quality ✅ Complete
- Input validation, timeouts, Jira error handling, SLF4J logging, JPA open-in-view disabled

## Phase 5 — QA Intelligence Layer ✅ Complete
- PostgreSQL, history API, intelligence endpoints, dashboard, root URL redirect

## Phase 6 — Full QA Lifecycle Automation ✅ Complete
- Jira webhook (In Progress → analysis, Done → release summary)
- LlmClient interface — Groq / Azure OpenAI / AWS Bedrock
- Released tickets dashboard section with QA verdict

## Phase 7 — Copilot Studio v2 ✅ Complete
- Swagger v3.0.0 — all endpoints documented
- agent_intelligence_summary — QA status in Teams
- agent_release_summary — release verdicts in Teams
- Power Automate connector updated to v3

## Phase 8 — Dashboard Intelligence ✅ Complete
- Search across all tables (issue key, feature summary)
- Filter by risk level (High / Medium / Low)
- Filter by release recommendation (Block / Caution / Go)
- Record count badges per section

## Phase 9 — Multi-tenant Foundation ✅ Complete
- TenantConfig model created
- Architecture documented and ready
- Per-customer Jira + LLM credential isolation designed
- Full DB isolation planned for first enterprise customer

## Phase 10 — Risk Trend Analysis ✅ Complete
- `/qa/api/v1/intelligence/trends` — risk trends across re-analyzed issues
- `/qa/api/v1/intelligence/trends/{issueKey}` — full risk timeline per ticket
- `/qa/api/v1/intelligence/reanalyzed` — most re-analyzed tickets
- `/qa/api/v1/intelligence/released/summary` — Copilot-friendly release summary
- Swagger v3 updated with all new endpoints

## Phase 11 — Multi-Agent Copilot Studio ✅ Complete
- 7 dedicated Copilot agents — each returns only its relevant stage
- Dedicated backend endpoints per agent (`/qa/api/v1/agent/*`)
- issueKey normalization — "project-8" → "PROJ-8" automatically
- Agent tools wired to focused endpoints — no full pipeline dumps
- Activity.Text extraction — no question nodes, seamless UX
- Power Automate connector updated with all agent endpoints

## Phase 12 — Database Migration ✅ Complete
- Migrated from Render PostgreSQL (free tier, expires) to Neon PostgreSQL
- Zero downtime migration — SPRING_DATASOURCE_URL updated in Render env vars
- Neon free tier — no expiry, production-grade, AWS US East 1
- All analysis history, intelligence data, and release records preserved going forward

## Phase 13 — Production Hardening 📋 Planned
- Azure / AWS deployment option
- SOC2 compliance preparation
- Rate limiting per tenant
- Audit logging
- Full multi-tenant DB isolation