# QA Orchestrator Platform — Architecture

## System Overview

QA Orchestrator Platform is an AI-powered QA decision engine that analyzes Jira issues and produces structured QA intelligence.

Each pipeline stage is powered by a large language model (LLM). Stages are not independent — each stage reads from the previous stage output and builds on it. This is what makes it a real pipeline, not a collection of isolated analyzers.

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
   Jira REST    Groq LLM  Neon PostgreSQL
      API          API
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

## Technology Stack

### Backend
- Java 17
- Spring Boot 3
- Maven

### LLM
- Groq API (Llama 3.3 70B) — default
- Pluggable via LlmClient interface — Azure OpenAI, AWS Bedrock supported

### Database
- Neon PostgreSQL (free tier, no expiry, AWS US East 1)

### Infrastructure
- Docker
- Render Cloud

### Integrations
- Jira REST API
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
│   ├── QaAnalyzeRequest.java         ← includes issueKey normalization
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
│   │   ├── GroqClient.java
│   │   ├── AzureOpenAiClient.java
│   │   └── AwsBedrockClient.java
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

**LLM client is pluggable** — `LlmClient` is an interface. Switch providers by changing `LLM_PROVIDER` env var — no code changes required.

**Graceful fallback on LLM error** — every stage has a fallback that prevents pipeline crash. If Groq fails on one stage, the pipeline continues with a default artifact.

**Dedicated agent endpoints** — `QaAgentController` exposes 5 focused endpoints (`/qa/api/v1/agent/*`). Each runs the full pipeline internally but returns only its relevant stage as plain text. Copilot agents call these directly.

**IssueKey normalization** — `IssueKeyNormalizer` converts any format ("project-8", "PROJECT-8", "proj-8") to canonical form ("PROJ-8") before Jira API calls.

**Single pipeline execution per request** — the controller calls `runAnalysis()` once. Pipeline does not run twice.

**Neon PostgreSQL** — migrated from Render free PostgreSQL (expires) to Neon free tier (no expiry). Zero downtime migration via env var update.

---

## Future Architecture

- QA Context Service — historical ticket and bug awareness per component
- Coverage-aware risk scoring — adjusts risk based on existing test coverage
- Release decision engine — structured go/no-go logic with override support
- Multi-tenant DB isolation — per-customer schema or database
- Rate limiting per tenant — prevent LLM abuse