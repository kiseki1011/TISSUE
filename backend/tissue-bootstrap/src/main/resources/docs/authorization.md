Tissue uses role-based access control (RBAC) at two scopes. The **workspace** and **project**.

## Workspace Roles

| Role     | Description                                      |
|----------|--------------------------------------------------|
| `OWNER`  | Full control including workspace deletion and ownership transfer. |
| `ADMIN`  | Can manage members, settings, and all resources within the workspace. |
| `MEMBER` | Can participate in projects and use workspace features. |

Only a single `OWNER` can exist in a workspace.

Hierarchy: `OWNER` > `ADMIN` > `MEMBER`

## Project Roles

| Role      | Description                                      |
|-----------|--------------------------------------------------|
| `MANAGER` | Can manage project settings, workflows, issue types, and project members. |
| `MEMBER`  | Can create and manage issues, comments, and other project resources. |

Hierarchy: `MANAGER` > `MEMBER`

## Default Access Rules

- Most project scoped APIs require **project membership** (`MEMBER` or higher).
- Administrative operations explicitly state the required role in the description.
- Workspace `ADMIN` or higher can perform most project scoped actions regardless of project role.
