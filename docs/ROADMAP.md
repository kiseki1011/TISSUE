# Roadmap for Tissue

> [!IMPORTANT]
> This roadmap is not finished. Feature specifications can change anytime.

## Optimization & Testing

- [ ] optimization
  - [ ] fix N+1 queries
  - [ ] caching
  - [ ] circular relation check needs optimization (currently using simple DFS)
- [ ] testing
  - [ ] integration tests for complex features
    - [ ] workflow
    - [x] issue create/update
    - [x] custom issue type creation
    - [ ] vcs automation
  - [ ] ≥80% test coverage for `tissue-core`
  - [ ] move tests to a private repo and run tests in CI (to block AI pr)
- [ ] migrate to GraalVM

---

## Core Features

### Authentication & Authorization

- [x] JWT based authentication with Spring Security
- [x] email login
- [x] OAuth 2.0 login
  - [ ] Google (needs e2e test)
  - [ ] GitHub (needs e2e test)
- [x] Role Based Access Control (workspace / project)
- [ ] feature level permission control (optional)
- [ ] SSO / 2FA (optional)

### Issue Tracking

- [x] issue CRUD
  - [x] file attachment
- [x] comments
  - [x] mentioning
- [x] custom issue types/fields CRUD
- [x] issue relations
  - [ ] circular dependency
- [x] Cross-project issue relations
  - [ ] Add caching for relation cycle detection
- [x] Review and approval workflow
- [ ] Issue tags
- [ ] Upload file to issue

### Workflow and Automation

- [x] custom workflows CRUD
  - [x] replace full workflow graph for update
- [x] transition guards (conditions)
  - [x] approval guard
  - [x] blocking issue guard
  - [ ] add additional guards
- [x] add `ABORTED` to `StateCategory`
- [x] ensure workflow graph consistency

### Sprint

- [x] sprint CRUD
- [x] sprint management
- [x] sprint issue migration

### Activity & Audit

- [x] activity history of issue and sprint
- [ ] cryptographic signing for activity logs (optional, doubt this will be needed)

### Notifications

- [x] in-app and email notifications
- [x] notification preference management
- [ ] notification integration
  - [ ] slack integration
  - [ ] Discord integration

### VCS

- [x] VCS integration
  - [ ] self-hosted
    - [ ] Gitea
    - [ ] Forgejo
  - [ ] 3rd party
    - [x] GitHub
    - [ ] GitLab
- [ ] CI integration
  - [ ] CICD status observability

### Data Management

- [ ] encrypted data storage
- [ ] backup and restore

### Markdown based Wiki

- [x] markdown file based wiki CRUD
  - [x] wiki file attachment
  - [x] document version tracking
- [ ] markdown to wiki document (by parsing frontmatter)
- [x] storage provider interface
  - [x] Local storage
  - [x] S3 compatible
- [ ] export PDF, MD

---

## Dashboard

- [ ] issues by workflow state (`StateCategory`)
- [ ] cycle time, lead time
- [ ] sprint velocity
- [ ] burndown chart

---

## Local-First

- [ ] offline, online detection
- [ ] local SQLite database
- [ ] mirror the backend (schema and operations)
- [ ] permission table
- [ ] permission verification
- [ ] how to solve conflict
  - [ ] detect conflict and solve (with version)
  - [ ] sync engine (considering)
    - [ ] open source sync engine (example: ElectricSQL)

---

## TUI Client

- [ ] authentication & connection flow
- [ ] API client
  - [ ] workspace
  - [ ] project
  - [ ] work items
- [ ] client-side state management
- [ ] sidebar (projects, sprints, members)
- [ ] dashboards
  - [ ] my issues
  - [ ] backlog
  - [ ] project overview chart
  - [ ] statistics
  - [ ] kanban (optional)
- [ ] command palette
- [ ] keyboard navigation
  - [ ] vim friendly keybindings
  - [ ] tmux friendly keybindings
- [ ] issue list view
- [ ] issue detail view (markdown rendering)
- [ ] issue create/edit forms
- [ ] support hyperlink
- [ ] view images
  - [ ] recommended: Terminal with graphic protocol (example: Kitty, Sixel)
  - [ ] provide fallback for terminals without graphic protocol
    - [ ] link: open through default(system) image viewer

---

## Data Portability

- [ ] full data export (JSON / CSV / Excel)
- [ ] Schema.org–compatible export format
- [ ] flexible data import
  - [ ] field mapping
  - [ ] loose validation
  - [ ] JSON fallback for unmapped fields

---

## AI Integration

- [ ] Local LLM integration (exmaple: Ollama)
- [ ] Read-only features
  - [ ] Issue summarization
  - [ ] Semantic issue search

---

## Observability

- [ ] structured application logging
- [ ] pluggable log exporters
  - [ ] loki
  - [ ] file / stdout
  - [ ] external log collectors
- [ ] basic metric monitoring

---

## Documentation

- [x] API documentation (openAPI)
- [ ] installation guide
  - [ ] self-hosting backend
  - [ ] client (TUI)
- [ ] user guide
- [ ] migration & data portability guide
- [ ] contribution guidelines

---

## Out of Scope

- AI write permissions
  - I personally think granting AI systems write access to production data is still considered risky. AI features will be limited to read-only operations or suggestions. (At least for now)
- Support of external AI services
  - I've thought a lot about this but, I want Tissue to serve as a successful testbed for proving the viability of local LLMs, so I don't plan to support external AI connections.
- Kubernetes support
  - Tissue prioritizes simple self-hosting over complex orchestration.
- SaaS hosting
  - Tissue focuses on self-hosted deployments and does not provide a managed SaaS.
