# QA Orchestrator Platform

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3-green)
![API](https://img.shields.io/badge/API-Live-brightgreen)
![Contract](https://img.shields.io/badge/Contract-v2-orange)
![LLM](https://img.shields.io/badge/LLM-Groq%20%2F%20Llama%203.3-purple)
![DB](https://img.shields.io/badge/DB-Neon%20PostgreSQL-blue)
![Copilot](https://img.shields.io/badge/Copilot-Studio-0078d4)

**QA Orchestrator Platform** is an AI-powered QA decision engine covering the full QA lifecycle — from ticket creation to release — integrated with Microsoft Copilot Studio and Power Automate as a 7-agent system.

---

## Live

| | URL |
|---|---|
| Dashboard | https://qa-orchestrator-service.onrender.com |
| API | https://qa-orchestrator-service.onrender.com/qa/api/v1/qa/analyze |
| Health | https://qa-orchestrator-service.onrender.com/qa/health |
| Website | https://vooabi.com |

---

## Full QA Lifecycle

```
Ticket → "In Progress" → QA pipeline runs automatically
                       → Requirement Analysis
                       → Test Design
                       → Automation Decision
                       → Risk Analysis
                       → Bug Report Template
                       → Saved to DB + Jira comment

Ticket → "Done"       → QA Release Summary generated
                       → Verdict: APPROVED / APPROVED WITH RISK / RELEASED WITHOUT FULL COVERAGE
                       → Saved to DB + Jira comment
```

---

## System Architecture

```
User / Copilot Studio / Power Automate / Jira Webhook
            │
            ▼
    QA Orchestrator API
            │
            ▼
     Spring Boot Service
       │           │           │
       ▼           ▼           ▼
  Jira REST    LLM API     Neon PostgreSQL
```

---

## Multi-Agent Copilot Studio

7 dedicated agents — each returns only its relevant stage output. No full pipeline dumps in chat.

| Agent | Trigger Example | Dedicated Endpoint |
|-------|----------------|--------------------|
| `agent_requirement_analyzer` | "What are the requirements for project-5" | `POST /qa/api/v1/agent/requirements` |
| `agent_test_case_generator` | "Create test cases for project-6" | `POST /qa/api/v1/agent/testcases` |
| `agent_risk_predictor` | "What is the risk for project-7" | `POST /qa/api/v1/agent/risk` |
| `agent_automation_builder` | "Automation strategy for project-8" | `POST /qa/api/v1/agent/automation` |
| `agent_bug_reporter` | "Create a bug report for project-5" | `POST /qa/api/v1/agent/bugreport` |
| `agent_intelligence_summary` | "Give me a QA summary" | `GET /qa/api/v1/intelligence/summary` |
| `agent_release_summary` | "Which tickets have been released" | `GET /qa/api/v1/intelligence/released/summary` |

Issue key normalization is automatic — "project-8" → "PROJ-8", "create a bug report for project-8" → "PROJ-8".

---

## LLM Provider Support

| Provider | Env Var | Model | Best For |
|----------|---------|-------|----------|
| Groq (default) | `LLM_PROVIDER=groq` | Llama 3.3 70B | Development, free tier |
| Azure OpenAI | `LLM_PROVIDER=azure` | GPT-4o | Enterprise, Microsoft ecosystem |
| AWS Bedrock | `LLM_PROVIDER=aws` | Claude 3.5 Sonnet | Enterprise, AWS ecosystem |

---

## QA Analysis Pipeline

| Stage | Output |
|-------|--------|
| Requirement Analysis | clarifiedRequirements, edgeCases, openQuestions, scope |
| Test Design | testScenarios, testCases (UI/API/E2E) |
| Automation Decision | automationRecommendation, coverageSplit, framework |
| Risk Analysis | riskScore (0-100), riskLevel, releaseRecommendation |
| Bug Report | title, severity, reproductionSteps, impactSummary |
| Release Summary | APPROVED / APPROVED WITH RISK / RELEASED WITHOUT FULL COVERAGE |

---

## Intelligence Dashboard

`https://qa-orchestrator-service.onrender.com`

- Metrics: total analyses, avg risk, blocked, released
- Risk distribution + release decision charts
- Recent analyses — searchable, filterable by risk and release
- Blocked tickets — searchable
- Released tickets with QA verdicts — searchable

---

## Jira Webhook

| Status | Action |
|--------|--------|
| `In Progress` | Full QA analysis pipeline |
| `Done` | QA release summary generation |

Setup: Jira → System → WebHooks → URL: `https://qa-orchestrator-service.onrender.com/qa/webhook/jira` → Event: `Issue updated` → JQL: `project = PROJ`

---

## API Endpoints

### Core

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Redirects to dashboard |
| GET | `/qa/health` | Health + intelligence summary |
| GET | `/qa/dashboard` | Intelligence Dashboard |
| POST | `/qa/api/v1/qa/analyze` | Full pipeline analysis |
| POST | `/qa/webhook/jira` | Jira webhook |

### Agent Endpoints (Copilot Studio)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/qa/api/v1/agent/requirements` | Requirements only |
| POST | `/qa/api/v1/agent/testcases` | Test cases only |
| POST | `/qa/api/v1/agent/risk` | Risk analysis only |
| POST | `/qa/api/v1/agent/automation` | Automation strategy only |
| POST | `/qa/api/v1/agent/bugreport` | Bug report only |

### History & Intelligence

| Method | Path | Description |
|--------|------|-------------|
| GET | `/qa/api/v1/history` | Last 10 analyses |
| GET | `/qa/api/v1/history/{issueKey}` | Per-issue history |
| GET | `/qa/api/v1/intelligence/summary` | Intelligence summary |
| GET | `/qa/api/v1/intelligence/high-risk` | HIGH risk analyses |
| GET | `/qa/api/v1/intelligence/blocked` | Blocked analyses |
| GET | `/qa/api/v1/intelligence/released` | Released tickets + QA summaries |
| GET | `/qa/api/v1/intelligence/released/summary` | Released summary (Copilot-friendly) |
| GET | `/qa/api/v1/intelligence/trends` | Risk trends across re-analyzed issues |
| GET | `/qa/api/v1/intelligence/trends/{issueKey}` | Risk timeline for specific issue |
| GET | `/qa/api/v1/intelligence/reanalyzed` | Most re-analyzed issues |

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `JIRA_BASE_URL` | Yes | Jira instance URL |
| `JIRA_EMAIL` | Yes | Jira account email |
| `JIRA_API_TOKEN` | Yes | Jira API token |
| `GROQ_API_KEY` | Yes (Groq) | Groq API key |
| `AZURE_OPENAI_KEY` | Yes (Azure) | Azure OpenAI key |
| `AZURE_OPENAI_ENDPOINT` | Yes (Azure) | Azure OpenAI endpoint |
| `AZURE_OPENAI_DEPLOYMENT` | No | Model deployment (default: gpt-4o) |
| `AWS_ACCESS_KEY` | Yes (AWS) | AWS access key |
| `AWS_SECRET_KEY` | Yes (AWS) | AWS secret key |
| `AWS_REGION` | No | AWS region (default: us-east-1) |
| `LLM_PROVIDER` | No | groq / azure / aws (default: groq) |
| `SPRING_DATASOURCE_URL` | Yes | Neon PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Yes | PostgreSQL username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | PostgreSQL password |
| `JIRA_COMMENT_ENABLED` | No | Post analysis to Jira comment (default: false) |

---

## Local Development

```bash
export JIRA_BASE_URL=https://your-domain.atlassian.net
export JIRA_EMAIL=your-email@example.com
export JIRA_API_TOKEN=your-jira-api-token
export GROQ_API_KEY=gsk_...

./mvnw spring-boot:run
```

```bash
curl -X POST http://localhost:10000/qa/api/v1/qa/analyze \
-H "Content-Type: application/json" \
-d '{"issueKey":"PROJ-4"}'
```

---

## Roadmap

| Phase | Status | Description |
|-------|--------|-------------|
| 1 | ✅ | Foundation — backend, Jira, pipeline, deployment |
| 2 | ✅ | LLM Intelligence — all stages AI-powered |
| 3 | ✅ | Observability — logging, health, error handling |
| 4 | ✅ | Hardening — validation, timeouts, security |
| 5 | ✅ | Intelligence layer — PostgreSQL, dashboard, history |
| 6 | ✅ | Full lifecycle — webhook, release summary, LLM providers |
| 7 | ✅ | Copilot Studio v2 — intelligence + release topics |
| 8 | ✅ | Dashboard — search, filtering, record counts |
| 9 | ✅ | Multi-tenant foundation — TenantConfig, architecture ready |
| 10 | ✅ | Risk trend analysis — timeline, trends, reanalyzed issues |
| 11 | ✅ | Multi-Agent Copilot Studio — 7 dedicated agents, focused endpoints |
| 12 | ✅ | Database migration — Render → Neon PostgreSQL (free, no expiry) |
| 13 | 📋 | Production hardening — Azure/AWS, SOC2, rate limiting |