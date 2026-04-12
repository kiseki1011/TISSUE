All API resources are scoped to one of three levels. The hierarchy is represented through the URL path.

## Resource Hierarchy

```
Global
├── Authentication, Member Account, Invitations
│
└── Workspace ({workspaceKey})
    ├── Team, Position, Project Template, Wiki, Notification, VCS Integration
    │
    └── Project ({projectKey})
        ├── Sprint, Workflow, Issue Type, Tag
        │
        └── Issue ({issueKey})
            ├── Comment, Issue Attachment
            └── Activity Log
```

## Scope Behavior

| Scope      | URL Pattern                                                       | Description                                                             |
|------------|-------------------------------------------------------------------|-------------------------------------------------------------------------|
| Global     | `/api/v1/auth/...`, `/api/v1/members/...`                         | Not tied to any workspace or project                                    |
| Workspace  | `/api/v1/workspaces/{workspaceKey}/...`                           | Belongs to a specific workspace                                         |
| Project    | `/api/v1/workspaces/{workspaceKey}/.../projects/{projectKey}/...` | Belongs to a specific project within a workspace                        |
| Issue      | `/api/v1/workspaces/{workspaceKey}/issues/{issueKey}/...`         | Issue is scoped under a specific project (`{projectKey}-{issueNumber}`) |

- Workspace scoped APIs require at least workspace `MEMBER` role
- Project scoped APIs require at least project `MEMBER` role
- Certain issue operations that require a project context (ex: issue creation, batch operations) use the project scoped path: `/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/issues`
- See the [Authorization](#tag/Authorization) guide for role details

## Key Conventions

| Key            | Format   | Uniqueness Scope | Description                       |
|----------------|----------|------------------|-----------------------------------|
| `workspaceKey` | `string` | Global           | Unique identifier for a workspace |
| `projectKey`   | `string` | Within workspace | Identifier for a project          |
| `issueKey`     | `string` | Within workspace | Identifier for an issue.          |

- Keys are **immutable** once created
- `projectKey` and `issueKey` are unique within their workspace, not globally
- Keys are used as path parameters throughout the API instead of numeric IDs. (Only for workspaces/projects/issues)
- The `issueKey` follows the format `{projectKey}-{issueNumber}`
