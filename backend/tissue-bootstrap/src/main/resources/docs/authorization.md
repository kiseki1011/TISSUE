Tissue uses role-based access control (RBAC) at two scopes: a **system** scope and a **project** scope.

## System Roles

Every member has exactly one system role.

| Role          | Description                                                                                       |
|---------------|---------------------------------------------------------------------------------------------------|
| `SUPER_ADMIN` | Full control of the deployment. The first registered member becomes one; at least one always exists. |
| `ADMIN`       | Server operator. Can override project-scoped actions across all projects (for recovery/support).  |
| `USER`        | Standard member. Access is governed by project roles.                                             |

Hierarchy: `SUPER_ADMIN` > `ADMIN` > `USER`

## Project Roles

A member's role within a specific project.

| Role      | Description                                                                |
|-----------|----------------------------------------------------------------------------|
| `MANAGER` | Can manage project settings, sprints, tags, VCS integrations, and project members. |
| `MEMBER`  | Can create and manage issues, comments, and other project resources.       |

Hierarchy: `MANAGER` > `MEMBER`

## Default Access Rules

- All project scoped APIs require at least project `MEMBER` role.
- System `ADMIN` or higher can perform project scoped actions regardless of project role (operator override).
- When an API requires a higher role, the **Requirements** section in the description explicitly states the required role.
