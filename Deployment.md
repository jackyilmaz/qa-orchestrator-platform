# QA Orchestrator — Deployment Guide

## Requirements

- Docker + Docker Compose installed
- 8GB RAM minimum (16GB recommended for Ollama)
- Jira account with API token

---

## Quick Start (Offline / On-Premise)

```bash
# 1. Clone the repo
git clone https://github.com/your-org/qa-orchestrator-platform.git
cd qa-orchestrator-platform

# 2. Set up environment
cp .env.example .env
# Edit .env with your Jira credentials

# 3. Start everything
docker-compose up -d

# 4. Pull the AI model (first time only — ~5GB download)
docker exec qa-ollama ollama pull llama3.3

# 5. Done — open the dashboard
open http://localhost:10000
```

---

## Quick Start (Cloud — Azure OpenAI)

```bash
# 1. Edit .env
LLM_PROVIDER=azure
AZURE_OPENAI_KEY=your-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_OPENAI_DEPLOYMENT=gpt-4o

# 2. Start without Ollama
docker-compose up -d qa-orchestrator postgres

# 3. Done
open http://localhost:10000
```

---

## Services

| Service | Port | Description |
|---------|------|-------------|
| QA Orchestrator | 10000 | Main API + Dashboard |
| PostgreSQL | 5432 | Database |
| Ollama | 11434 | Local LLM (offline mode) |

---

## Switching LLM Provider

Edit `.env` and change `LLM_PROVIDER`:

| Value | Provider | Internet Required |
|-------|----------|-------------------|
| `ollama` | Local Llama 3.3 | No |
| `azure` | Azure OpenAI GPT-4o | Yes |
| `groq` | Groq Llama 3.3 70B | Yes |
| `aws` | AWS Bedrock Claude 3.5 | Yes |

Then restart: `docker-compose restart qa-orchestrator`

---

## Jira Webhook Setup (optional — auto-triggers analysis)

1. Go to Jira → Settings → System → WebHooks
2. Create webhook → URL: `http://your-server-ip:10000/qa/webhook/jira`
3. Event: Issue updated
4. JQL: `project = YOUR-PROJECT`

---

## Data Persistence

All data is stored in Docker volumes and persists across restarts:
- `postgres_data` — all analysis history, intelligence data, release records
- `ollama_data` — downloaded AI models (no re-download needed after restart)

---

## Stopping / Starting

```bash
# Stop
docker-compose down

# Start
docker-compose up -d

# View logs
docker-compose logs -f qa-orchestrator

# Restart single service
docker-compose restart qa-orchestrator
```

---

## Updating

```bash
git pull
docker-compose build qa-orchestrator
docker-compose up -d qa-orchestrator
```