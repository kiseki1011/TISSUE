All API resources are scoped to one of three levels. The hierarchy is represented through the URL path.

## Resource Hierarchy

```
Global
├── Authentication, Member Account, Workflow, Issue Type, Wiki, Notification
│
└── Project ({projectKey})
    ├── Project Member, Sprint, Tag, VCS Integration
    │
    └── Issue ({issueKey})
        ├── Comment, Issue Attachment
        └── Activity Log
```

## Scope Behavior

| Scope   | URL Pattern                           | Description                                                             |
|---------|---------------------------------------|-------------------------------------------------------------------------|
| Global  | `/api/v1/members/...`, `/api/v1/projects`, `/api/v1/workflows`, `/api/v1/issue-types`, `/api/v1/wiki/...`, `/api/v1/notifications/...` | Not tied to any specific project    |
| Project | `/api/v1/projects/{projectKey}/...`   | Belongs to a specific project                                           |
| Issue   | `/api/v1/issues/{issueKey}/...`       | Issue is scoped under a specific project (`{projectKey}-{issueNumber}`) |

- Project scoped APIs require at least project `MEMBER` role; system `ADMIN`/`SUPER_ADMIN` may override across all projects
- Certain issue operations that require a project context (ex: issue creation, batch operations) use the project scoped path: `/api/v1/projects/{projectKey}/issues`
- See the [Authorization](#tag/Authorization) guide for role details

## Key Conventions

| Key          | Format   | Uniqueness Scope | Description              |
|--------------|----------|------------------|--------------------------|
| `projectKey` | `string` | Global           | Identifier for a project |
| `issueKey`   | `string` | Global           | Identifier for an issue. |

- Keys are **immutable** once created
- `projectKey` and `issueKey` are **globally unique**
- Keys are used as path parameters throughout the API instead of numeric IDs. (Only for projects/issues)
- The `issueKey` follows the format `{projectKey}-{issueNumber}`
