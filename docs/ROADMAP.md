# Roadmap for Tissue

> [!IMPORTANT]
> This roadmap is not finished. Feature specifications can change anytime.

## Auth

- [x] JWT based authentication with Spring Security
- [x] OAuth 2.0 login
  - [x] Google
  - [x] GitHub
- [ ] Workspace/Project level RBAC
- [ ] SSO(OIDC)
- [ ] feature level permission control (optional)

## Issue Tracking

- [x] Issue CRUD
  - [x] File attachment
- [x] Issue query API (basic / common / custom / parent / children / relations / author / reviewers / subscribers / available transitions)
- [x] Issue search API (project-scoped, Specification-based with priority / state / tag / sprint / date-range / progress / keyword filters)
  - [ ] workspace-scoped search
  - [ ] full-text keyword search over content (LOB)
    - separate `IssueFullTextSearchRepository` using PostgreSQL `tsvector` GIN index
- [x] Comments
  - [x] 1-depth constraint
  - [x] Mention
- [x] Custom issue types/fields CRUD
  - [x] EAV → JSONB
- [x] Issue relations
  - [x] Circular dependency
    - [ ] Needs to change implementation due to performance problems
      - Intend to using caching
  - [x] Cross-project issue relations
- [x] Review and approval workflow
- [x] Issue tags

## Workflow and Automation

- [x] Custom workflow engine
  - [x] Replace full workflow graph for update
  - [ ] Ensure workflow graph consistency (validation)
  - [x] Transition guards (conditions)
    - [x] Approval guard
    - [x] Blocking issue guard
    - [ ] Add additional guards

## Sprint

- [x] Sprint CRUD
- [x] Sprint management
- [x] Sprint issue migration

## Activity & Audit

- [x] Activity history (`ActivityLog`) of issue and sprint
- [ ] cryptographic signing for activity logs (optional, doubt this will be needed)

## Notifications

- [x] In-app and email (smtp) notifications
- [x] Notification preference management
- [ ] Notification integration
  - [ ] Slack integration
  - [ ] Discord integration
  - [ ] Etc

## VCS

- [x] VCS integration
  - [ ] self-hosted
    - [ ] Gitea
    - [ ] Forgejo
  - [ ] 3rd-party
    - [x] GitHub
    - [ ] GitLab
- [ ] CI integration
  - [ ] CICD status observability

## Wiki

- [x] Markdown file based wiki CRUD
  - [x] Wiki file attachment
  - [x] Document version tracking
  - [ ] Text search
    - [ ] Performance improvement using PostgreSQL `tsvector`
- [ ] Markdown to wiki document (by parsing frontmatter)
- [x] Storage provider interface
  - [x] Local storage
  - [x] S3 compatible
- [ ] Export PDF, MD

## Data & Schema Management

- [ ] Encrypted data storage
- [ ] Backup and restore
- [ ] Flyway

## Dashboard

- [ ] Issues by workflow state (`StateCategory`)
- [ ] Cycle time, lead time
- [ ] Sprint velocity
- [ ] Burndown chart
- [ ] Gannt chart

## Data Portability

- [ ] Full data export (JSON / CSV / Excel)
- [ ] Schema.org–compatible export format
- [ ] Flexible data import
  - [ ] Field mapping
  - [ ] Loose validation
  - [ ] JSON fallback for unmapped fields

## AI Integration

- [ ] MCP server
- [ ] Issue summarization
- [ ] Semantic issue search


## Observability

- [ ] Structured application logging
- [ ] Pluggable log exporters
  - [ ] loki
  - [ ] file / stdout
  - [ ] external log collectors
- [ ] Basic metric monitoring


## Testing & Optimization

- [ ] Fix N+1 queries
- [ ] Caching
- [ ] Testing
  - [ ] Integration tests for complex features
    - [ ] Workflow
    - [x] Issue create/update
    - [x] Custom issue type creation
    - [ ] Vcs automation
  - [ ] ≥80% test coverage
- [ ] Migrate to GraalVM
  - [ ] Needs to remove reflection if needed
- [ ] Performance check
  - [ ] k6 P99, P95

## Documentation

- [x] API documentation (openAPI)
- [ ] Installation guide
  - [ ] Self-hosting backend
  - [ ] Client (TUI)
- [ ] User guide
- [ ] Migration & data portability guide
- [ ] Contribution guidelines
