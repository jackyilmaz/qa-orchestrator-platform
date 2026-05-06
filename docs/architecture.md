# QA Orchestrator Platform — Architecture

## System Overview

QA Orchestrator Platform is an AI-powered QA decision engine that analyzes Jira issues and produces structured QA intelligence.

Each pipeline stage is powered by a large language model (LLM). Stages are not independent — each stage reads from the previous stage output and builds on it. This is what makes it a real pipeline, not a collection of isolated analyzers.

The system is deployment-agnostic — it runs identically on cloud, private cloud, GovCloud, or fully air-gapped on-premise environments. All configuration is done via environment variables.

---

## High Level Architecture

```
User / Copilot Studio / Power Automate / Jira Webhook
            │
            ▼
      Custom API Connector
            │
            ▼
      QA Orchestrator API
            │
            ▼
      Spring Boot Service
         │         │         │
         ▼         ▼         ▼
   Jira REST   LLM Provider  PostgreSQL
      API      (pluggable)   (Neon or on-prem)
```

---

## Multi-Agent Architecture

Each Copilot Studio agent calls a dedicated backend endpoint that returns only its relevant stage. No full pipeline dumps in chat.

```
Microsoft Teams (natural language)
        │
        ▼
Copilot Studio Agent (detects intent, extracts issueKey)
        │
        ▼
Dedicated Agent Endpoint (/qa/api/v1/agent/*)
        │
        ▼
QA Orchestrator Service (runs full pipeline)
        │
        ▼
Focused Stage Response (only relevant stage returned)
```

---

## Pipeline Flow

```
Jira Issue JSON
      │
      ▼
RequirementAnalysisStage
  → reads: raw Jira JSON
  → produces: clarifiedRequirements, edgeCases, openQuestions, scope
      │
      ▼
TestDesignStage
  → reads: clarifiedRequirements, edgeCases
  → produces: testScenarios, testCases (with UI/API/E2E types)
      │
      ▼
AutomationDecisionStage
  → reads: testCases (type distribution), riskLevel, scope
  → produces: automationRecommendation, coverageSplit, frameworkSuggestion
      │
      ▼
RiskAnalysisStage
  → reads: requirements, testCases, openQuestions, scope
  → produces: riskScore, riskLevel, topRiskDrivers, releaseRecommendation
      │
      ▼
BugReportStage
  → reads: full pipeline context
  → produces: bug report template (title, severity, reproductionSteps, impactSummary)
      │
      ▼
AnalysisSummaryStage
  → reads: key fields from all stages
  → produces: analysisSummary (human-readable one-liner)
      │
      ▼
StageAggregationStage
  → packages all stage artifacts into analysis.stages
      │
      ▼
Structured QA Response (analysis.stages)
```

---

## LLM Provider Architecture

The `LlmClient` interface decouples the pipeline from any specific LLM provider. Switching providers requires only a single env var change — no code changes.

```
QaOrchestratorService
        │
        ▼
   LlmClient (interface)
        │
        ├── AzureOpenAiClient   (LLM_PROVIDER=azure)  — GPT-4o, cloud default
        ├── GroqClient          (LLM_PROVIDER=groq)   — Llama 3.3 70B, fast/free
        ├── AwsBedrockClient    (LLM_PROVIDER=aws)    — Claude 3.5, GovCloud
        └── OllamaClient        (LLM_PROVIDER=ollama) — any local model, fully offline
```

---

## Deployment Options

| Mode | LLM | Database | Infrastructure | Target |
|------|-----|----------|----------------|--------|
| Cloud SaaS | Azure OpenAI | Neon PostgreSQL | Render Cloud | Default |
| Private Cloud | Azure / AWS | Any PostgreSQL | Customer cloud | Enterprise |
| GovCloud | AWS Bedrock | Any PostgreSQL | AWS GovCloud | Government |
| Air-Gapped | Ollama (local) | On-prem PostgreSQL | Customer servers | Military / Regulated |

All modes use the same Docker image and Java codebase. The only difference is which environment variables are set.

---

## Technology Stack

### Backend
- Java 17
- Spring Boot 3
- Maven

### LLM
- Azure OpenAI (GPT-4o) — active cloud provider
- Groq (Llama 3.3 70B) — fast, free tier alternative
- AWS Bedrock (Claude 3.5 Sonnet) — enterprise, GovCloud ready
- Ollama — self-hosted, fully offline, air-gapped capable
- All pluggable via `LlmClient` interface — switch with `LLM_PROVIDER` env var

### Database
- Neon PostgreSQL — cloud default (free tier, no expiry)
- Any PostgreSQL instance supported — on-premise, RDS, Cloud SQL

### Infrastructure
- Docker — single container, runs anywhere
- Render Cloud — current cloud deployment
- Any server or cloud — Docker makes this portable

### Integrations
- Jira REST API (cloud or on-premise)
- Microsoft Copilot Studio (7 dedicated agents)
- Power Automate (custom connector, Swagger v3)

---

## Package Structure

```
com.qa.qa_orchestrator_service
├── controller
│   ├── QaController.java
│   ├── QaAgentController.java        ← dedicated Copilot agent endpoints
│   ├── HistoryController.java
│   ├── TrendController.java
│   ├── DashboardController.java
│   ├── HealthController.java
│   ├── JiraWebhookController.java
│   └── RootController.java
├── jira
│   └── JiraClient.java
├── model
│   ├── QaAnalysisResult.java
│   ├── QaAnalyzeRequest.java
│   ├── QaAnalyzeResponse.java
│   ├── QaStagesArtifact.java
│   ├── QaTestCase.java
│   ├── RequirementStageArtifact.java
│   ├── TestDesignStageArtifact.java
│   ├── AutomationStageArtifact.java
│   ├── RiskStageArtifact.java
│   └── BugReportStageArtifact.java
├── repository
│   ├── AnalysisRecord.java
│   └── AnalysisRecordRepository.java
├── service
│   ├── QaOrchestratorService.java
│   ├── AnalysisRecordService.java
│   ├── PipelineLogger.java
│   ├── llm
│   │   ├── LlmClient.java            ← interface
│   │   ├── AzureOpenAiClient.java    ← LLM_PROVIDER=azure
│   │   ├── GroqClient.java           ← LLM_PROVIDER=groq
│   │   ├── AwsBedrockClient.java     ← LLM_PROVIDER=aws
│   │   └── OllamaClient.java         ← LLM_PROVIDER=ollama (offline)
│   └── stage
│       ├── RequirementAnalysisStage.java
│       ├── TestDesignStage.java
│       ├── AutomationDecisionStage.java
│       ├── RiskAnalysisStage.java
│       ├── BugReportStage.java
│       ├── ReleaseSummaryStage.java
│       ├── AnalysisSummaryStage.java
│       └── StageAggregationStage.java
├── tenant
│   └── TenantConfig.java
└── util
    └── IssueKeyNormalizer.java
```

---

## Key Design Decisions

**Stages feed each other** — TestDesignStage does not re-read the Jira JSON. It reads `clarifiedRequirements` and `edgeCases` from RequirementStage output. This is the core pipeline pattern.

**LLM client is pluggable** — `LlmClient` is an interface. Four providers supported: Azure OpenAI, Groq, AWS Bedrock, Ollama. Switch by changing `LLM_PROVIDER` env var — no code changes required.

**Ollama for offline/air-gapped** — `OllamaClient` calls a locally running Ollama server. Zero internet required after initial model download. Supports Llama 3.3, Mistral, Phi-3, CodeLlama, and any model Ollama supports.

**Azure OpenAI as active provider** — GPT-4o is the current active model. Groq (Llama 3.3 70B) and AWS Bedrock (Claude 3.5 Sonnet) are supported alternatives.

**Graceful fallback on LLM error** — every stage has a fallback that prevents pipeline crash.

**Dedicated agent endpoints** — `QaAgentController` exposes 5 focused endpoints. Each runs the full pipeline internally but returns only its relevant stage as plain text.

**IssueKey normalization** — `IssueKeyNormalizer` converts any format to canonical form before Jira API calls.

**Single pipeline execution per request** — the controller calls `runAnalysis()` once.

**Neon PostgreSQL** — migrated from Render free PostgreSQL to Neon free tier (no expiry). On-premise PostgreSQL also supported via JDBC URL.

**Docker-first** — the entire service runs in a single Docker container. This is what makes on-premise and air-gapped deployment possible with no architectural changes.

---

## Future Architecture

- QA Context Service — historical ticket and bug awareness per component
- Coverage-aware risk scoring — adjusts risk based on existing test coverage
- Release decision engine — structured go/no-go logic with override support
- Multi-tenant DB isolation — per-customer schema or database
- Rate limiting per tenant — prevent LLM abuse
- On-premise deployment guide — step-by-step IT team documentation