# QA Orchestrator Platform — API Contract

## Overview

This document defines the API contract for the QA Orchestrator Platform.

The platform analyzes Jira issues and produces structured QA outputs across 5 LLM-powered stages:

- Requirement analysis
- Test design
- Automation decision
- Risk evaluation
- Bug report template

Current contract version: **v2**

---

## Base URL

| Environment | URL |
|-------------|-----|
| Production | https://qa-orchestrator-service.onrender.com |
| Local | http://localhost:10000 |

---

## Keep-Alive Endpoint

### GET `/ping`

Lightweight endpoint used by cron jobs to keep the Render service warm.
Does **not** touch the database. Returns immediately.

**Response**
```json
{ "status": "OK" }
```

Use this endpoint for external monitoring and keep-alive pings — not `/qa/health`, which triggers a database query on every call.

---

## Primary Endpoint

### POST `/qa/api/v1/qa/analyze`

Analyzes a Jira issue and returns structured QA insights across all 5 stages.

**Request Headers**
```
Content-Type: application/json
```

**Request Body**
```json
{
  "issueKey": "PROJ-4"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| issueKey | string | Yes | Jira issue key. Normalized automatically — "project-4" → "PROJ-4" |

---

## Agent Endpoints (Copilot Studio)

Each agent endpoint runs the full pipeline internally but returns only its relevant stage as plain text. Designed for Microsoft Copilot Studio agent tools.

| Method | Path | Returns |
|--------|------|---------|
| POST | `/qa/api/v1/agent/requirements` | Requirement analysis only |
| POST | `/qa/api/v1/agent/testcases` | Test cases only |
| POST | `/qa/api/v1/agent/risk` | Risk analysis only |
| POST | `/qa/api/v1/agent/automation` | Automation strategy only |
| POST | `/qa/api/v1/agent/bugreport` | Bug report only |

**Request Body (all agent endpoints)**
```json
{
  "issueKey": "PROJ-5"
}
```

**Response (all agent endpoints)**
```json
{
  "result": "Formatted plain text output for the requested stage"
}
```

---

## Response Structure (Full Analysis)

```json
{
  "output": "Requirement status: READY. Automation: Hybrid. Risk: MEDIUM (60). Release decision: Caution.",
  "analysis": {
    "contractVersion": "v2",
    "traceabilityId": "PROJ-4",
    "analysisSummary": "Requirement status: READY. Automation: Hybrid. Risk: MEDIUM (60). Release decision: Caution.",
    "stages": {
      "requirement": {},
      "testDesign": {},
      "automation": {},
      "risk": {},
      "bugReport": {}
    }
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| output | string | Human-readable summary line |
| analysis | object | Structured QA analysis object |
| analysis.contractVersion | string | Contract version (currently v2) |
| analysis.traceabilityId | string | Jira issue key |
| analysis.analysisSummary | string | One-line QA decision summary |
| analysis.stages | object | Canonical stage output — primary consumer path |

---

## Canonical Path

All structured output lives under:

```
analysis.stages
```

---

# Stage Artifacts

---

## Requirement Stage

Path: `analysis.stages.requirement`

```json
{
  "status": "READY",
  "featureSummary": "Apply a coupon code during checkout to receive a discount.",
  "clarifiedRequirements": [
    "User can enter a coupon code in the coupon field.",
    "Valid coupon applies discount to subtotal before tax.",
    "Invalid coupon displays error message.",
    "Only one coupon can be active at a time."
  ],
  "edgeCases": [
    "Empty coupon code field submitted.",
    "Expired coupon code entered.",
    "Coupon code with maximum usage limit reached."
  ],
  "openQuestions": [
    "What is the format of a valid coupon code?",
    "How does the system handle different discount types?"
  ],
  "scope": [
    "Coupon code field on the checkout page",
    "Discount application to subtotal before tax"
  ],
  "outOfScope": [
    "Coupon code generation and management",
    "Integration with third-party coupon services"
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| status | string | READY or BLOCKED |
| featureSummary | string | One-sentence feature description |
| clarifiedRequirements | string[] | Testable requirements extracted from ticket |
| edgeCases | string[] | Boundary and negative scenarios |
| openQuestions | string[] | Gaps that could block test execution |
| scope | string[] | In-scope system areas |
| outOfScope | string[] | Explicitly excluded areas |

---

## Test Design Stage

Path: `analysis.stages.testDesign`

```json
{
  "testScenarios": [
    "Valid Coupon Application",
    "Invalid Coupon Handling",
    "Expired Coupon",
    "Empty Field Submission",
    "Multiple Coupon Restriction"
  ],
  "testCases": [
    {
      "id": "TC-01",
      "title": "Apply Valid Coupon Code",
      "preconditions": "User is on checkout page with items in cart",
      "steps": [
        "Enter valid coupon code in coupon field",
        "Click apply button",
        "Verify discount applied to subtotal"
      ],
      "expectedResult": "Discount applied, order total updated",
      "testType": "UI",
      "suiteTag": "Smoke",
      "testData": "Valid coupon code: SAVE10",
      "priority": "High"
    }
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| testScenarios | string[] | High-level test scenario names |
| testCases | object[] | Structured, execution-ready test cases |
| testCases[].id | string | Test case ID (TC-01, TC-02...) |
| testCases[].title | string | Descriptive test case title |
| testCases[].preconditions | string | Required state before test runs |
| testCases[].steps | string[] | Step-by-step execution instructions |
| testCases[].expectedResult | string | Verifiable expected outcome |
| testCases[].testType | string | UI / API / E2E |
| testCases[].suiteTag | string | Smoke / Regression |
| testCases[].testData | string | Specific test data used |
| testCases[].priority | string | High / Medium / Low |

---

## Automation Stage

Path: `analysis.stages.automation`

```json
{
  "automationRecommendation": "Hybrid (UI + API)",
  "automationReasoning": "The feature has both user-facing interactions and backend validation logic.",
  "coverageSplit": "UI 60% / API 40%",
  "frameworkSuggestion": "Java + Selenium + TestNG + REST Assured"
}
```

| Field | Type | Description |
|-------|------|-------------|
| automationRecommendation | string | Manual / UI-heavy / API-heavy / Hybrid |
| automationReasoning | string | Explanation of the strategy choice |
| coverageSplit | string | UI vs API percentage |
| frameworkSuggestion | string | Recommended test framework |

---

## Risk Stage

Path: `analysis.stages.risk`

```json
{
  "riskScore": 60,
  "riskLevel": "MEDIUM",
  "riskReason": "Feature impacts critical checkout path with financial implications.",
  "topRiskDrivers": [
    "Impact on critical user path (checkout)",
    "Financial impacts (discount calculation)",
    "Unresolved open questions affecting test coverage"
  ],
  "releaseRecommendation": "Caution"
}
```

| Field | Type | Description |
|-------|------|-------------|
| riskScore | integer | 0–100 risk score |
| riskLevel | string | LOW / MEDIUM / HIGH |
| riskReason | string | One-sentence risk explanation |
| topRiskDrivers | string[] | Key risk contributors |
| releaseRecommendation | string | Go / Caution / Block |

---

## Bug Report Stage

Path: `analysis.stages.bugReport`

```json
{
  "title": "Coupon Code Application Failure During Checkout",
  "environment": "QA / Staging",
  "severity": "Medium",
  "priority": "P3",
  "reproductionSteps": [
    "Navigate to checkout page with items in cart",
    "Enter a valid coupon code in the coupon field",
    "Click the apply button",
    "Observe the result"
  ],
  "expectedResult": "Discount applied to subtotal, order total updated.",
  "actualResult": "To be filled by QA engineer after test execution.",
  "impactSummary": "Incorrect discounts could cause financial loss and poor user experience.",
  "affectedAreas": [
    "Coupon code field on checkout page",
    "Discount calculation logic",
    "Order total display"
  ],
  "suggestedAssignee": "Backend Developer"
}
```

| Field | Type | Description |
|-------|------|-------------|
| title | string | Descriptive bug report title |
| environment | string | Target test environment |
| severity | string | Critical / High / Medium / Low |
| priority | string | P1 / P2 / P3 / P4 |
| reproductionSteps | string[] | Steps to reproduce the defect |
| expectedResult | string | What should happen |
| actualResult | string | What actually happened (filled by QA) |
| impactSummary | string | Business impact if bug exists |
| affectedAreas | string[] | System components at risk |
| suggestedAssignee | string | Recommended assignee role |

---

# Backward Compatibility Fields

Top-level flat fields are retained for earlier integrations:

- `analysis.requirementStatus`
- `analysis.featureSummary`
- `analysis.automationRecommendation`
- `analysis.riskLevel`
- `analysis.riskScore`
- `analysis.releaseRecommendation`
- `analysis.clarifiedRequirements`
- `analysis.edgeCases`
- `analysis.testScenarios`
- `analysis.testCases`

Legacy stage aliases also present:

- `analysis.requirementStage`
- `analysis.testDesignStage`
- `analysis.automationStage`
- `analysis.riskStage`
- `analysis.bugReportStage`

These exist only for compatibility. Canonical consumers should use `analysis.stages`.

---

# Contract Versioning

| Rule | Description |
|------|-------------|
| Breaking changes | Require new contract version |
| New fields | Can be added without version bump |
| Canonical path | Always `analysis.stages` |

---

# History & Intelligence Endpoints

## GET `/qa/api/v1/history`
Returns the last 10 analysis records.

## GET `/qa/api/v1/history/{issueKey}`
Returns all analysis records for a specific Jira issue key.

## GET `/qa/api/v1/intelligence/summary`
Returns aggregated intelligence summary across all analyses.

```json
{
  "totalAnalyses": 10,
  "averageRiskScore": 78,
  "highRiskCount": 7,
  "blockedCount": 5,
  "mostAnalyzedIssues": [
    { "issueKey": "PROJ-5", "count": 3 },
    { "issueKey": "PROJ-6", "count": 2 }
  ]
}
```

## GET `/qa/api/v1/intelligence/high-risk`
Returns all analyses with riskLevel = HIGH, ordered by risk score descending.

## GET `/qa/api/v1/intelligence/blocked`
Returns all analyses with releaseRecommendation = Block, ordered by most recent first.

## GET `/qa/api/v1/intelligence/released`
Returns all released tickets with QA verdicts.

## GET `/qa/api/v1/intelligence/released/summary`
Returns a Copilot-friendly plain text summary of released tickets.

## GET `/qa/api/v1/intelligence/trends`
Returns risk trends across re-analyzed issues.

## GET `/qa/api/v1/intelligence/trends/{issueKey}`
Returns full risk score timeline for a specific issue.

## GET `/qa/api/v1/intelligence/reanalyzed`
Returns most re-analyzed issues ordered by analysis count.

---

# Local Testing

```bash
export JIRA_BASE_URL=https://your-domain.atlassian.net
export JIRA_EMAIL=your-email@example.com
export JIRA_API_TOKEN=your-jira-api-token
export AZURE_OPENAI_KEY=your-azure-openai-key
export AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
export AZURE_OPENAI_DEPLOYMENT=gpt-4o
export LLM_PROVIDER=azure

./mvnw spring-boot:run
```

```bash
curl -X POST http://localhost:10000/qa/api/v1/qa/analyze \
-H "Content-Type: application/json" \
-d '{"issueKey":"PROJ-4"}'
```

---

# Supported Integrations

- QA workflow orchestration
- AI-assisted test planning
- Release decision support
- Microsoft Copilot Studio (7 dedicated agents)
- Power Automate (custom connector, Swagger v3)
- Jira webhook automation
- Dashboard visualization