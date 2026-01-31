# Roadmap for Tissue

> [!IMPORTANT]
> This roadmap is not finished. Feature specifications can change anytime.
>
> The most focused goals are:
>
> - Flexible data import/export with a easy to use UI(TUI) for data portability
> - Save operations in local(offline) and sync when online
> - Use local LLM models (example: Ollama) for data sovereignty

## 1. Backend Architecture

- [ ] Modularize backend (`common`, `domain`, `infra`, `api`, `notification`, `admin`)
- [ ] Enforce module boundaries (no circular dependencies)
- [ ] Improve code readability and structure
- [ ] Performance optimization
  - [ ] Fix N+1 queries
  - [ ] Introduce caching
- [ ] Testing
  - [x] Integration tests with Testcontainers
  - [ ] Integration tests for critical or complex features
  - [ ] ≥80% unit test coverage for `domain`
- [ ] Migrate to GraalVM (strongly considering)

---

## 2. Core Backend Features

### Authentication & Authorization

- [x] JWT-based authentication
- [x] Email login
- [x] OAuth 2.0 login
  - [ ] Google
  - [ ] GitHub
- [x] Role Based Access Control (workspace / project)
- [ ] Feature level permission control
- [ ] SSO / 2FA (optional)

### Workspace & Project Management

- [x] Workspace / Project CRUD
- [x] Invitations and member management (including roles)
- [x] Teams and positions for workspace members
- [ ] Project template

### Issue Tracking

- [x] Issue CRUD, priority, comments
- [x] Custom issue types and fields
- [x] Reporter / Assignee / Reviewer / Subscriber
- [x] Issue hierarchy (Epic → Standard → Subtask → Microtask, is set through issue type)
- [x] Cross-project issue relations
  - [ ] Add caching for relation cycle detection
- [x] Review and approval workflow
- [ ] Issue tags
- [ ] Upload file to issue

### Workflow and Automation

- [x] Custom workflows (states & transitions)
- [x] Transition guards
  - [x] Approval guard
  - [x] Blocking issue guard
  - [ ] Additional guards
- [ ] Refine workflow state categories
  - [ ] Add `CANCELED` to `StateCategory`
    - [ ] Ensure workflow graph consistency

### Sprint & Planning

- [x] Sprint management
- [x] Sprint issue migration

### Activity & Audit

- [x] Activity history of issue and sprint
- [ ] Cryptographic signing for activity logs (considering)

### Notifications & Integrations

- [x] In-app and email notifications
- [ ] Slack integration
- [ ] Discord integration
- [x] VCS integration
  - [ ] Self-hosted Git
    - [ ] Gitea
    - [ ] Forgejo
  - [x] GitHub
  - [ ] GitLab

### Data Management

- [ ] Encrypted data storage and governance
- [ ] Backup and restore

### Simple Built-In Wiki

- [ ] Simple markdown file based wiki
- [ ] Storage provider interface
  - [ ] Local storage
  - [ ] S3 compatible opensource (example: MinIO, Garage)
- [ ] Export PDF, MD

---

## 3. Basic Statistics

- [ ] Open issue count over time
- [ ] Issues by workflow state
- [ ] Cycle time, lead time
- [ ] Sprint velocity
- [ ] Burndown chart

---

## 4. Local-First

- [ ] Local SQLite database
- [ ] Mirror the backend (schema and operations)
- [ ] Permission table
- [ ] Sync engine
  - [ ] Sync
  - [ ] Conflict detection and resolution (including permission verification)
- [ ] Use a open source sync-engine (example: ElectricSQL) (sort of considering)

---

## 5. TUI Client

### Core

- [x] Authentication & connection flow
- [ ] API client
  - [ ] Workspace
  - [ ] Project
  - [ ] Work items
- [ ] Client-side state management

### Navigation & UX

- [ ] Sidebar (projects, sprints, members)
- [ ] Dashboards
  - [ ] My issues
  - [ ] Backlog
  - [ ] Project overview chart
  - [ ] Basic statistics
  - [ ] Kanban (optional)
- [ ] Command palette
- [ ] Keyboard-first navigation
- [ ] Vim-style keybindings
- [ ] Tmux friendly workflow, keybindings

### Issue UI

- [ ] Issue list view
- [ ] Issue detail view (Markdown rendering)
- [ ] Issue create/edit forms
- [ ] Support hyperlink
- [ ] View images
  - [ ] Recommended: Terminal with graphic protocol (example: Kitty, Sixel)
  - [ ] Provide fallback for terminals without graphic protocol
    - [ ] Link: open through default(system) image viewer

---

## 6. Data Portability

- [ ] Full data export (JSON / CSV / Excel)
- [ ] Schema.org–compatible export format
- [ ] Flexible data import
  - [ ] Field mapping
  - [ ] Loose validation
  - [ ] JSON fallback for unmapped fields

---

## 7. AI Integration

- [ ] Local LLM integration (exmaple: Ollama)
- [ ] Read-only features
  - [ ] Issue summarization
  - [ ] Semantic issue search

---

## 8. Observability

- [ ] Structured application logging
- [ ] Log level configuration
- [ ] Pluggable log exporters
  - [ ] Loki (recommended default)
  - [ ] File / stdout
  - [ ] External log collectors
- [ ] Monitoring: basic metrics

---

## 9. DevOps & Deployment

- [x] Docker Compose (app + DB + cache + storage)
- [ ] Production Docker image
- [ ] install.sh
- [ ] Terraform (optional)

---

## 10. Documentation

- [ ] API documentation (Swagger)
- [ ] Installation guide
  - [ ] Self-hosting
  - [ ] Client(TUI)
- [ ] User guide
- [ ] Migration & data portability guide
- [ ] Contribution guidelines

---

## 11. Playground

- [ ] Provide a simple playground server to test features without self-hosting (optional)

---

## Might Consider in Future

- ActivityPub integration

## Out of Scope

- AI write permissions
  - I personally think granting AI systems write access to production data is still considered risky. AI features will be limited to read-only operations or suggestions. (At least for now)
- Support of external AI services
  - I've thought a lot about this but, I want Tissue to serve as a successful testbed for proving the viability of local LLMs, so I don't plan to support external AI connections.
- Kubernetes support
  - Tissue prioritizes simple self-hosting over complex orchestration.
- SaaS hosting
  - Tissue focuses on self-hosted deployments and does not provide a managed SaaS.
