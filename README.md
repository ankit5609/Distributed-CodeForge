# 🚀 Distributed CodeForge

<div align="center">

<!-- Core Platform -->
[![Java](https://img.shields.io/badge/Java-21-FF6B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.15-6DB33F.svg?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2025.0.3-6DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![Spring AI](https://img.shields.io/badge/Spring_AI-1.1.8-6DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)

<!-- Infrastructure -->
[![Kubernetes](https://img.shields.io/badge/Kubernetes-GKE-326CE5.svg?style=for-the-badge&logo=kubernetes&logoColor=white)](https://kubernetes.io/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED.svg?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-CI%2FCD-2088FF.svg?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/features/actions)
[![Google Cloud](https://img.shields.io/badge/GCP-Workload_Identity-4285F4.svg?style=for-the-badge&logo=googlecloud&logoColor=white)](https://cloud.google.com/)

<!-- Data & Messaging -->
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-Event_Driven-231F20.svg?style=for-the-badge&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16_+_pgvector-4169E1.svg?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Route_Cache-DC382D.svg?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![MinIO](https://img.shields.io/badge/MinIO-Object_Storage-C72C48.svg?style=for-the-badge&logo=minio&logoColor=white)](https://min.io/)

<!-- AI & Payments -->
[![OpenRouter](https://img.shields.io/badge/OpenRouter-LLM_Gateway-8B5CF6.svg?style=for-the-badge&logo=openai&logoColor=white)](https://openrouter.ai/)
[![pgvector](https://img.shields.io/badge/pgvector-RAG_Retrieval-4169E1.svg?style=for-the-badge&logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![Stripe](https://img.shields.io/badge/Stripe-Payments-635BFF.svg?style=for-the-badge&logo=stripe&logoColor=white)](https://stripe.com/)

<!-- Frontend -->
[![React](https://img.shields.io/badge/React-18-61DAFB.svg?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5-646CFF.svg?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-v4-06B6D4.svg?style=for-the-badge&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)

<!-- Meta -->
[![License](https://img.shields.io/badge/License-MIT-F59E0B.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)
[![Production](https://img.shields.io/badge/Status-Production_Ready-10B981.svg?style=for-the-badge)]()
[![Architecture](https://img.shields.io/badge/Architecture-Microservices-EC4899.svg?style=for-the-badge)]()

<br/>

> **A cloud-native, AI-powered full-stack application development and instant Kubernetes sandbox preview platform.**
> Build, preview, and ship — all from your browser.

[🎨 Architecture](#-system-architecture) &nbsp;•&nbsp; [📁 Repository Map](#-repository-map) &nbsp;•&nbsp; [⚡ Execution Flows](#-execution-flows) &nbsp;•&nbsp; [🚀 Quick Start](#-quick-start) &nbsp;•&nbsp; [🔄 CI/CD](#-cicd-pipeline) &nbsp;•&nbsp; [📜 API Reference](#-api-reference)

</div>

---

## 📖 Overview

**Distributed CodeForge** is a world-class, cloud-native full-stack application generation and preview sandbox platform. Built on a modular microservices architecture, it features a state-of-the-art AI code generation and execution engine:

- 💻 **Real-Time Collaborative Coding** — Multi-user project access with real-time directory updates and role-based permissions.
- 🧠 **Spring AI & LLM Engine** — Streaming LLM chat using OpenAI drivers via OpenRouter, featuring a strict XML prompt protocol for planning and execution.
- 🐘 **pgvector-Powered RAG Engine** — Context injection utilizing PostgreSQL `pgvector` semantic search, injecting project file trees and relevant code snippets dynamically.
- 👁️ **Multimodal Visual Diagnostics** — Support for visual bug diagnostics and design replication using image/screenshot attachments via `multipart/form-data` streams.
- 🩺 **Compile-Error Self-Healing Loop** — Run-time preview builder verifying code syntax, searching past build-error solutions in `pgvector`, and recording resolution diffs to self-heal compiler crashes.
- 🏗️ **Instant Kubernetes Sandbox Previews** — Isolated runner pods with pre-warmed standby pools, MinIO file syncing, and subdomain-based proxy routing.
- 💳 **Integrated Billing** — Stripe checkout, subscription plans, and token usage enforcement.

---

## 🎨 System Architecture

Distributed CodeForge is split into **two Kubernetes namespaces** that are network-isolated from each other:
- `codeforge-core` — the stateless control plane housing all microservices, databases, and the message bus
- `codeforge-previews` — the dynamic sandbox execution plane for isolated runner pods

```mermaid
flowchart LR
    %% ── Node Styles ─────────────────────────────────────────────────────────
    classDef client   fill:#0F172A,stroke:#38BDF8,color:#F8FAFC,font-weight:bold
    classDef ingress  fill:#1E1B4B,stroke:#818CF8,color:#F8FAFC,font-weight:bold
    classDef frontend fill:#14532D,stroke:#4ADE80,color:#F8FAFC
    classDef gateway  fill:#1E3A5F,stroke:#60A5FA,color:#F8FAFC,font-weight:bold
    classDef cfgsvc   fill:#1C1917,stroke:#D4A26A,color:#F8FAFC
    classDef svc      fill:#14532D,stroke:#4ADE80,color:#F8FAFC
    classDef kafka    fill:#431407,stroke:#FB923C,color:#F8FAFC,font-weight:bold
    classDef db       fill:#2E1065,stroke:#C084FC,color:#F8FAFC
    classDef proxy    fill:#172554,stroke:#38BDF8,color:#F8FAFC,font-weight:bold
    classDef runner   fill:#0C1A2E,stroke:#60A5FA,color:#F8FAFC

    %% ── Entry Point ─────────────────────────────────────────────────────────
    BROWSER(["🌐 Browser Client"]):::client

    NGINX["⚡ NGINX Ingress
    /api/*  →  API Gateway
    /*      →  Frontend
    *.previews.*  →  Proxy"]:::ingress

    BROWSER -->|"HTTPS / WSS"| NGINX

    %% ── codeforge-core Namespace ────────────────────────────────────────────
    subgraph CORE ["  ☸️  codeforge-core  "]
        direction TB

        %% Tier 0 — Static Frontend
        FE["🖥️ codeforge-frontend
        :80  ·  Nginx + React SPA"]:::frontend

        %% Tier 1 — Infrastructure Services
        subgraph INFRA_ROW ["  📡 Infrastructure  "]
            direction LR
            CFG["⚙️ config-service
            :8888  ·  Spring Cloud Config
            Git-backed profiles"]:::cfgsvc
            EUR["🔍 discovery-service
            :8761  ·  Eureka Registry"]:::cfgsvc
        end

        %% Tier 2 — Gateway
        GW["🛡️ api-gateway  :80
        JWT Validation  ·  Load Balancer"]:::gateway

        %% Tier 3 — Domain Services
        subgraph SVCS ["  🔧 Microservices  "]
            direction LR
            ACC["👤 account-service
            :9010
            Auth · Stripe · Email"]:::svc
            WS["📁 workspace-service
            :9020
            Files · K8s · MinIO"]:::svc
            INT["🧠 intelligence-service
            :9030
            LLM · RAG · pgvector"]:::svc
        end

        %% Tier 4 — Messaging
        KAFKA[["🔁 Apache Kafka
        ─────────────────────────
        file-storage-request-event
        notification-events"]]:::kafka

        %% Tier 5 — Data
        subgraph DATA ["  🗄️ Data Layer  "]
            direction LR
            PG[("🐘 PostgreSQL 16
            + pgvector
            account · workspace · intel")]:::db
            RD[("⚡ Redis
            Route cache
            TTL metrics")]:::db
            MN[("📦 MinIO
            Object storage
            Project files")]:::db
        end
    end

    %% ── codeforge-previews Namespace ────────────────────────────────────────
    subgraph PREV ["  🏗️  codeforge-previews  "]
        direction TB
        PROX["🔀 codeforge-me-proxy
        :80  ·  Subdomain router"]:::proxy
        PODS["🚀 Runner Pod Pool
        npm run dev :5173
        Syncer sidecar"]:::runner
    end

    %% ── Routing ─────────────────────────────────────────────────────────────
    NGINX -->|"Static assets"| FE
    NGINX -->|"Authenticated API"| GW
    NGINX -->|"Sandbox subdomain"| PROX

    %% ── Config & Discovery (passive) ────────────────────────────────────────
    CFG -.->|"Profiles at startup"| GW
    CFG -.->|"Profiles at startup"| ACC
    CFG -.->|"Profiles at startup"| WS
    CFG -.->|"Profiles at startup"| INT
    EUR -.->|"Registry lookup"| GW

    %% ── Gateway → Services ──────────────────────────────────────────────────
    GW -->|"REST"| ACC
    GW -->|"REST"| WS
    GW -->|"REST + SSE stream"| INT

    %% ── Services → Data ─────────────────────────────────────────────────────
    ACC <-->|"CRUD"| PG
    WS  <-->|"CRUD"| PG
    INT <-->|"Chat · Vector search"| PG
    WS  <-->|"File R/W"| MN
    WS  <-->|"Route keys"| RD

    %% ── Kafka Event Bus ─────────────────────────────────────────────────────
    INT -->|"FileStoreRequestEvent"| KAFKA
    ACC -->|"NotificationEvent"| KAFKA
    KAFKA -->|"Idempotent consume"| WS

    %% ── Sandbox Plane ───────────────────────────────────────────────────────
    WS   -->|"Fabric8 · Claim pod"| PODS
    PODS -->|"Mirror files"| MN
    PROX <-->|"Route lookup"| RD
    PROX -->|"Forward TCP / WS"| PODS
```

---


## 📁 Repository Map

| Directory | Sub-component | Port | Responsibility |
| :--- | :--- | :---: | :--- |
| 🛡️ `api-gateway/` | Gateway Router | `80` | Dynamic request routing, JWT security filters, load balancing via Eureka. |
| ⚙️ `config-service/` | Configuration Server | `8888` | Central Git-backed Spring Cloud Config Server distributing profiles to all services. |
| 🔍 `discovery-service/` | Eureka Registry | `8761` | Service registration and discovery for dynamic inter-service resolution. |
| 👤 `account-service/` | Identity & Billing | `9010` | User auth (JWT), Stripe checkout flows, billing portals, and email notifications. |
| 📁 `workspace-service/` | File Engine & K8s | `9020` | Workspace metadata, file trees, MinIO file sync, and Fabric8 sandbox orchestration. |
| 🧠 `intelligence-service/` | AI & Vector Search | `9030` | LLM streaming, chat history, pgvector RAG indexing, and token usage tracking. |
| 📦 `common-lib/` | Shared Library | — | Shared DTOs, Kafka event contracts, JWT models, and global exception handlers. |
| 🌐 `codeforge-frontend/` | React SPA | `80` | Vite/React frontend with real-time chat, file editor, and sandbox preview panel. |
| ☸️ `k8s/` | Deployment Manifests | — | Kubernetes YAML manifests split into `/infra`, `/services`, `/stateful`, and `/proxy`. |

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
| :--- | :--- | :--- |
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.5.15 |
| **Microservices** | Spring Cloud (Gateway, Config, Feign, Eureka) | 2025.0.3 |
| **AI Integration** | Spring AI (OpenAI driver via OpenRouter) | 1.1.8 |
| **Frontend** | React + Vite + Tailwind CSS | 18 / 5 / v4 |
| **Database** | PostgreSQL + pgvector extension | 16 |
| **Cache / Routes** | Redis | 7 |
| **Messaging** | Apache Kafka | 3.x |
| **Object Storage** | MinIO | RELEASE.2024 |
| **Orchestration** | Kubernetes (GKE) via Fabric8 | GKE / 7.3.1 |
| **Build / Packaging** | Maven + Google Jib | 3.x / 3.4.3 |
| **Payments** | Stripe SDK | latest |

---

## ⚡ Execution Flows

### 1. Sandbox Preview Claim

```
Browser → api-gateway → workspace-service /projects/{id}/deploy
                                |
              ┌─────────────────┴──────────────────┐
              ▼                                    ▼
       Idle pod available?                  Pool exhausted
              │                                    │
              ▼                                    ▼
     Claim idle pod (IDLE→BUSY)       Evict oldest BUSY pod (LRU)
              │
              ▼
   Syncer sidecar pulls files from MinIO
              │
              ▼
   Runner container: nohup npm run dev (:5173)
              │
              ▼
   Redis key: "route:project-{id}.domain" → podIP:5173
              │
              ▼
   Return preview URL to browser
```

### 2. Event-Driven File Edit Saga

```
[Chat Prompt] → intelligence-service
                      │
                      │ parse <file> XML tags from LLM stream
                      ▼
              FileStoreRequestEvent (sagaId, path, content)
                      │
                      ▼
              Kafka: "file-storage-request-event"
                      │
                      ▼
              workspace-service (idempotency: check sagaId in DB)
                      │
                      ▼
              Save file to MinIO
                      │
                      ▼
              Kafka reply: "file-store-responses" → saga complete
```

### 3. Email Notification Dispatch (Kafka Consumer)

```
Trigger Event                         Kafka Topic                  Consumer
──────────────────────────────────────────────────────────────────────────────
SUBSCRIPTION_CREATED/CANCELLED  ──►  "notification-events"  ──►  account-service
TOKEN_LIMIT_REACHED                                               NotificationEventConsumer
                                                                        │
                                                                        ▼
                                                             JavaMailSender (Brevo SMTP)
```

### 4. RAG Context Injection for AI Chat

1. **File Indexing**: When files are saved, `intelligence-service` receives an internal call to chunk content and index it into `pgvector`.
2. **Query Enrichment**: On each chat request, a `FileTreeContextAdvisor` classifies the intent:
   - Regular queries → semantic search (`topK=5`)
   - Bug reports with image uploads → aggressive retrieval (`topK=10`) + filename matching
3. **Prompt Assembly**: Relevant file chunks are prepended to the LLM system prompt as context before streaming begins.

---

## 🧠 LLM, pgvector RAG, & Compile-Error Self-Healing

Distributed CodeForge integrates a highly advanced artificial intelligence suite powered by **Spring AI**, **pgvector Vector Store**, and custom tool-calling agents.

### 1. Spring AI & LLM Prompt Protocol
All LLM prompts are handled by the `intelligence-service` using the Spring AI framework configured with the OpenAI model driver connected to OpenRouter (`AI_API_KEY`).
* **XML Prompt Protocol**: The LLM operates under a strict persona template defined in `PromptUtils.java`. It must format all streaming output in XML structures:
  - `<tool args="paths">message</tool>`: Declares that the model will read files. Must precede calls to the `read_files` tool.
  - `<message phase="start|planning|completed">...</message>`: Explains planning and outcomes.
  - `<file path="path/to/file">...</file>`: Holds the complete file content. No placeholders or partial code are allowed.
* **Atomic Updates Constraint**: To enforce clean updates and prevent repetitive code churn, the model can output a specific `<file path="...">` exactly once per response.

### 2. Retrieval-Augmented Generation (RAG) Architecture
Rather than dumping the entire repository context window into the LLM (which is slow and expensive), the system implements a dynamic RAG pipeline inside `FileTreeContextAdvisor.java` which implements the Spring AI `StreamAdvisor` interface:
1. **Active File Tree Injection**: A lightweight representation of the active workspace paths is fetched from `workspace-service` via a Feign Client and appended as a system message.
2. **PostgreSQL pgvector Similarity Search**: The last user message is vectorized and matched against project files in the `intelligence_db`'s vector store (`vectorStore.similaritySearch`). The lookup uses dynamic metadata filtering: `projectId == {projectId}`.
3. **Intent-based Top-K Escalation**:
   - **Regular Queries**: Fetches the top `5` matching code blocks.
   - **Visual Bug Reports / Layout issues**: If the question matches `isBugReport` (contains keywords like "bug", "spacing", "disappear", "crash", "wrong"), the system escalates retrieval to `topK = 10` to get broader code coverage.
4. **Explicit File Resolution**: For bug reports, the system splits the prompt terms and matches them against the file tree. Any matching filenames or paths are aggressively loaded using `workspaceClient.getFileContent` and appended as raw files inside the system context.

### 3. Multimodal Visual Diagnostics
* **Image Upload**: Users can drag and drop screenshots (e.g., of a frontend compiler error, alignment issue, or layout mock).
* **Multipart Processing**: The frontend streams requests as `multipart/form-data`. The image is saved to MinIO and served as a relative URL (`/api/v1/workspace/projects/{id}/files/attachments/...`).
* **Intent Bifurcation**: The system prompt instructs the multimodal LLM:
  - **Intent A (Visual Bug Diagnostic)**: If the prompt describes a layout bug or misalignment, compare the image against the retrieved code context to pinpoint the bug, compile a fix, and write the corrected code.
  - **Intent B (Design Replication)**: If the prompt is a UI design screenshot, reconstruct it by writing matching React/Vite/Tailwind code.

### 4. Compile-Error Self-Healing Loop (Feedback Loop)
The platform features an autonomous compile-verification and self-correcting loop implemented in `CodeGenerationTools.java`:
1. **Tool Invocation**: After generating or editing code, the LLM is instructed to run the `deploy_and_verify_preview` tool.
2. **Build Monitoring**: The tool triggers a GKE rollout via `workspace-service` Feign client. It polls deployment status and logs for 15 iterations (30 seconds total).
3. **Error Capture & Healing**: If the state is `CRASHED` (e.g., TypeScript or Vite compiler error), the tool returns the compilation logs back to the LLM. 
4. **pgvector Fix Archives**:
   - Before returning the error logs, the tool queries pgvector for any past build fixes using a threshold of `0.7` similarity. If a matching past fix exists, the metadata diff is injected as a `HINT`.
   - The LLM updates code using the hint, then calls `deploy_and_verify_preview` again.
   - Once the build returns `SUCCESS`, the tool snapshots the workspace files, computes a simple diff (before vs after), and stores the compilation error text along with the successful diff as a document in pgvector with tag `type: 'error_fix'`. This creates an in-memory learning loop for developer workspace errors.

---

## 🗄️ Database Design

### `account_db`
| Table | Purpose |
| :--- | :--- |
| `users` | Credentials, billing IDs, reset token columns, timestamps |
| `plans` | Billing plan definitions (pricing, token limits, features) |
| `subscriptions` | Maps users to active Stripe subscriptions |
| `stripe_events` | Idempotency store for Stripe webhook event IDs |

### `workspace_db`
| Table | Purpose |
| :--- | :--- |
| `projects` | Project metadata, owner IDs, MinIO bucket assignments |
| `project_files` | File hierarchy metadata and sizes |
| `project_members` | Collaborators with role-based permissions |
| `previews` | Container assignments, status, and endpoints |
| `processed_events` | Saga event log for Kafka idempotency (`sagaId`) |

### `intelligence_db`
| Table | Purpose |
| :--- | :--- |
| `chat_sessions` | Scoped by `userId` + `projectId` composite key |
| `chat_messages` | Full conversation history with raw `content` column |
| `chat_events` | Granular streaming events: `THOUGHT`, `MESSAGE`, `FILE_EDIT`, `TOOL_LOG` |
| `usage_logs` | Daily token tracking per user for quota enforcement |

---

## 📜 API Reference

### Auth (`account-service`)
| Method | Endpoint | Description |
| :---: | :--- | :--- |
| `POST` | `/auth/signup` | Register new user, returns JWT |
| `POST` | `/auth/login` | Authenticate credentials, returns JWT |
| `POST` | `/auth/forgot-password` | Request password reset email |
| `POST` | `/auth/reset-password` | Reset password using reset token |

### Billing (`account-service`)
| Method | Endpoint | Description |
| :---: | :--- | :--- |
| `GET` | `/me/subscription` | Fetch active user subscription |
| `POST` | `/payments/checkout` | Create Stripe Checkout Session |
| `POST` | `/payments/portal` | Create customer billing portal link |
| `POST` | `/webhooks/payment` | Stripe payment webhook receiver |

### Projects (`workspace-service`)
| Method | Endpoint | Description |
| :---: | :--- | :--- |
| `GET` | `/projects` | List all projects user is a member of |
| `POST` | `/projects` | Create a new project workspace |
| `PATCH` | `/projects/{id}` | Edit project metadata |
| `DELETE` | `/projects/{id}` | Soft-delete a project |
| `POST` | `/projects/{id}/deploy` | Claim a sandbox pod and start dev server |
| `GET` | `/projects/{id}/logs` | Stream dev server logs from runner |

### Workspace Files (`workspace-service`)
| Method | Endpoint | Description |
| :---: | :--- | :--- |
| `GET` | `/projects/{id}/files` | Fetch full hierarchical file tree |
| `GET` | `/projects/{id}/files/content` | Fetch file content by relative path |

### Intelligence (`intelligence-service`)
| Method | Endpoint | Description |
| :---: | :--- | :--- |
| `POST` | `/chat/stream` | Stream AI chat (multipart: `message` text + optional `image`) |
| `GET` | `/chat/projects/{projectId}` | Fetch full chat history for a project |
| `POST` | `/internal/v1/embeddings/reindex` | Index file path + content into pgvector |

---

## 🔐 Environment Variables

All services fetch configuration centrally from `config-service`. Secrets are injected at deploy-time via Kubernetes Secrets.

| Variable | Secret Ref | Description |
| :--- | :--- | :--- |
| `JWT_SECRET` | `app-secrets` | HMAC key used for signing and validating JWT tokens |
| `STRIPE_API_KEY` | `app-secrets` | Stripe live/test API key for payment processing |
| `STRIPE_WEBHOOK_SECRET` | `app-secrets` | Signature verification secret for incoming Stripe webhooks |
| `AI_API_KEY` | `app-secrets` | OpenRouter API key for LLM completions |
| `MAIL_USERNAME` | `app-secrets` | SMTP username (Brevo) for transactional email dispatch |
| `MAIL_PASSWORD` | `app-secrets` | SMTP password for transactional email dispatch |
| `CONFIG_SERVER_URL` | ConfigMap | URL of the central Spring Cloud Config Server |
| `PREVIEW_DOMAIN` | ConfigMap | Base domain for sandbox wildcard routes |
| `PREVIEW_NAMESPACE` | ConfigMap | Target namespace for user sandboxes (`codeforge-previews`) |

---

## 🚀 Quick Start

### Prerequisites

| Tool | Min Version | Install |
| :--- | :--- | :--- |
| Java (JDK) | 21 | [adoptium.net](https://adoptium.net/) |
| Maven | 3.9+ | [maven.apache.org](https://maven.apache.org/) |
| Docker Desktop | 4.x+ | [docker.com](https://www.docker.com/products/docker-desktop/) |
| kubectl | 1.28+ | [kubernetes.io](https://kubernetes.io/docs/tasks/tools/) |
| Kind | 0.22+ | [kind.sigs.k8s.io](https://kind.sigs.k8s.io/) |

---

### Option A: Local Cluster with Kind

Run the entire platform locally using **Kind (Kubernetes in Docker)** — no cloud required.

#### Step 1 — Create the Kind Cluster

Create `kind-config.yaml` at the project root:

```yaml
apiVersion: kind.x-k8s.io/v1alpha4
kind: Cluster
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
```

```bash
kind create cluster --name codeforge --config kind-config.yaml
```

#### Step 2 — Install NGINX Ingress

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# Wait for ingress controller to be ready
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
```

#### Step 3 — Build Service Images

```bash
# Compile and package all microservices (skip tests for speed)
mvn clean install -DskipTests

# Build Docker images locally using Jib
mvn compile jib:dockerBuild
```

#### Step 4 — Load Images into Kind

```bash
kind load docker-image ankit5609/codeforge-account-service:v1 --name codeforge
kind load docker-image ankit5609/codeforge-workspace-service:v1 --name codeforge
kind load docker-image ankit5609/codeforge-intelligence-service:v1 --name codeforge
kind load docker-image ankit5609/codeforge-frontend:v1 --name codeforge
```

#### Step 5 — Create Kubernetes Secrets

```bash
kubectl create namespace codeforge-core
kubectl create namespace codeforge-previews

kubectl create secret generic app-secrets \
  --from-literal=JWT_SECRET=your_jwt_secret \
  --from-literal=STRIPE_API_KEY=sk_test_... \
  --from-literal=STRIPE_WEBHOOK_SECRET=whsec_... \
  --from-literal=AI_API_KEY=sk-or-v1-... \
  --from-literal=MAIL_USERNAME=your_smtp_user \
  --from-literal=MAIL_PASSWORD=your_smtp_password \
  -n codeforge-core
```

#### Step 6 — Deploy Everything

```bash
# Apply manifests in dependency order
kubectl apply -f k8s/infra/namespaces.yaml
kubectl apply -f k8s/stateful/          # Postgres, Redis, Kafka, MinIO
kubectl apply -f k8s/services/          # All microservices
kubectl apply -f k8s/proxy/             # Subdomain proxy
kubectl apply -f k8s/infra/             # Runner pool, network policies, ingress

# Monitor startup
kubectl get pods -n codeforge-core -w
```

#### Step 7 — Scale Sandbox Pool

```bash
kubectl scale deployment runner-pool --replicas=3 -n codeforge-previews
```

> **Tip**: Add `codeforge.local` and `*.previews.codeforge.local` to your `/etc/hosts` file pointing to `127.0.0.1` to test subdomain routing locally.

---

### Option B: GKE (Production)

For production deployment on Google Kubernetes Engine:

#### Step 1 — Authenticate & Configure

```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
gcloud container clusters get-credentials YOUR_CLUSTER_NAME --region YOUR_REGION
```

#### Step 2 — Build & Push Images via Jib

Jib compiles and pushes images directly to Docker Hub without requiring a local Docker daemon:

```bash
# Authenticate Docker
docker login

# Build and push all services
mvn compile jib:build -Djib.to.image=ankit5609/codeforge-account-service:v1
mvn compile jib:build -Djib.to.image=ankit5609/codeforge-workspace-service:v1
mvn compile jib:build -Djib.to.image=ankit5609/codeforge-intelligence-service:v1

# Build and push frontend manually
docker build --platform linux/amd64 -t ankit5609/codeforge-frontend:v1 codeforge-frontend/
docker push ankit5609/codeforge-frontend:v1
```

#### Step 3 — Create Secrets & Apply Manifests

```bash
kubectl create secret generic app-secrets \
  --from-literal=JWT_SECRET=your_jwt_secret \
  --from-literal=STRIPE_API_KEY=sk_live_... \
  --from-literal=AI_API_KEY=sk-or-v1-... \
  -n codeforge-core

kubectl apply -f k8s/infra/namespaces.yaml
kubectl apply -f k8s/stateful/
kubectl apply -f k8s/services/
kubectl apply -f k8s/proxy/
kubectl apply -f k8s/infra/
```

#### Step 4 — Wake Up Services

```bash
./start-cluster.sh
```

---

## 🔧 Useful Commands

```bash
# Check all pods across namespaces
kubectl get pods -A

# Scale sandbox pool up/down
kubectl scale deployment runner-pool --replicas=5 -n codeforge-previews

# Stream logs from a service
kubectl logs deployment/workspace-service -n codeforge-core -f

# Run database query in-cluster
kubectl exec pgvector-0 -n codeforge-core -- \
  psql -U postgres -d intelligence_db -c "SELECT * FROM chat_messages ORDER BY id DESC LIMIT 5;"

# Force restart a deployment
kubectl rollout restart deployment intelligence-service -n codeforge-core

# Delete the local Kind cluster
kind delete cluster --name codeforge
```

---

## ⚙️ Recent Bug Fixes & Refactoring

| # | Fix | Impact |
| :---: | :--- | :--- |
| 1 | **Actuator Health Recovery** — disabled mail health probe (`management.health.mail.enabled: false`) | Prevented `account-service` crash loops when Brevo SMTP credentials expire |
| 2 | **Subscription NonUniqueResult** — returned list from subscription repo, sorted by highest ID | Fixed Hibernate exception when users had both `DEMO` and `ACTIVE` subscription rows |
| 3 | **Always-FormData Chats** — rebuilt stream client to always use `FormData` format | Fixed `415 Unsupported Media Type` and enabled direct image file uploads |
| 4 | **Trailing-Slash 404** — removed trailing slash from `InternalWorkspaceController` `@RequestMapping` | Fixed Feign client routing to internal workspace endpoints for image uploads |
| 5 | **XML Fallback Parser** — added `MESSAGE`/`FILE_EDIT` event fallback when no XML tags found in raw LLM output | Fixed blank assistant reply bubbles when AI responded with plain Markdown |
| 6 | **ChatEventType Import** — added missing `ChatEventType` enum import in `ChatPanel.tsx` | Fixed `ChatEventType is not defined` ReferenceError that crashed the workspace |

---

## 🛡️ Security

- **Namespace Isolation** — microservices in `codeforge-core`, user sandboxes in `codeforge-previews` (no crossing)
- **Network Policies** — sandbox pods block outbound connections to private RFC ranges (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`)
- **JWT Guard Filters** — all requests validated at the API Gateway via shared Spring Security + `JwtAuthFilter`
- **Database Idempotency** — Stripe webhooks and Kafka Saga edits check event history before execution to prevent double-writes

---

## ⚡ Performance

- **Pre-warmed Pod Pool** — standby runner pods eliminate cold starts; idle pods are claimed in milliseconds instead of spawning new containers
- **Redis Route Cache** — subdomain-to-pod routing stored in Redis for sub-millisecond lookups by the proxy
- **LRU Sandbox Eviction** — when the pool is full, the oldest active pod is reclaimed and recycled to the idle pool
- **Kafka Async Sagas** — file edits are processed asynchronously, preventing AI streaming from blocking on file I/O

---

## 🔄 CI/CD Pipeline

Every microservice has a dedicated **GitHub Actions** workflow that triggers automatically on push to `main` — but **only when files within that service directory change** (path-filtered triggers), avoiding unnecessary builds.

### Workflows

| Workflow File | Trigger Path | Service |
| :--- | :--- | :--- |
| `deploy-account-service.yaml` | `account-service/**` | account-service |
| `deploy-workspace-service.yaml` | `workspace-service/**` | workspace-service |
| `deploy-intelligence-service.yaml` | `intelligence-service/**` | intelligence-service |
| `deploy-config-service.yaml` | `config-service/**` | config-service |
| `deploy-api-gateway.yaml` | `api-gateway/**` | api-gateway |

### Pipeline Stages (per service)

```
git push → main
      │
      ▼
① Checkout code
      │
      ▼
② Set up JDK 21 (Temurin) + Maven cache
      │
      ▼
③ Build common-lib (shared dependency)
      │
      ▼
④ Jib compile → push Docker image to DockerHub
      │   Image tagged: docker.io/ankit5609/<service>:<git-sha>
      │   Also tagged: latest
      │   No Docker daemon needed (Jib builds directly)
      │
      ▼
⑤ Auth to GCP via Workload Identity Federation (keyless — no SA key file)
      │
      ▼
⑥ Get GKE cluster credentials
      │
      ▼
⑦ kubectl set image → rolling update on GKE
      │
      ▼
⑧ kubectl rollout status → pipeline blocks until pods are healthy ✅
```

### Required GitHub Secrets

| Secret | Description |
| :--- | :--- |
| `DOCKERHUB_USERNAME` | Docker Hub username for Jib image push |
| `DOCKERHUB_TOKEN` | Docker Hub access token for Jib image push |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | GCP Workload Identity Federation provider resource name |
| `GCP_SERVICE_ACCOUNT` | GCP service account email with GKE deploy permissions |
| `GCP_CLUSTER` | Name of your GKE cluster |
| `GCP_ZONE` | Zone/region of your GKE cluster |

> **Keyless Auth**: The pipeline uses [GCP Workload Identity Federation](https://cloud.google.com/iam/docs/workload-identity-federation) instead of long-lived service account JSON keys. The GitHub Actions OIDC token is exchanged for short-lived GCP credentials — no secrets stored on disk.

### Config Server

The `config-service` reads all application profiles from a **dedicated private GitHub repository** (`codeforge-config-server`). Each microservice fetches its environment-specific YAML config (database URLs, API keys, feature flags) from this repo at startup via Spring Cloud Config. Updating config is as simple as pushing to that repo — no service restart required for most settings.

---

## ⚠️ Known Limitations

1. **Single-Instance Databases** — Postgres, Redis, Kafka, and MinIO run as single StatefulSets with no replication or active failover
2. **No Auto-scaling** — the sandbox runner pool must be manually scaled via `kubectl scale` or the `start-cluster.sh` helper
3. **Synchronous PGVector Indexing** — file chunk indexing happens synchronously on save; background batch reindexing is not yet implemented

---

## 📜 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Built with ☕ Java, ⚛️ React, and ☸️ Kubernetes &nbsp;|&nbsp; **Distributed CodeForge**

</div>
