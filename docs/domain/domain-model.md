# Domain Model & Business Rules

_This document describes the structural design, aggregate boundaries, and business constraints (invariants) of the system.
It serves as a guide for understanding implementation decisions and logic._

---

## 1. Core Aggregates

The system is designed around several key Aggregates that enforce consistency.

### 1.1 Issue Aggregate

The central aggregate of the system.

- **Root**: `Issue`
- **Components**:
  - `IssueContent`: Title, Description, Summary.
  - `IssueSchedule`: Due dates, Start/End timestamps.
  - `IssueParticipants`: Reporter, Assignee, Reviewers, Subscribers.
  - `IssueRelations`: Links to other issues (Blocks, Duplicates, etc.).
  - `IssueProgress`: Calculated progress based on children or checklist.
- **Key Responsibilities**:
  - Managing its own state transitions.
  - Enforcing parent-child hierarchy rules.
  - Preventing circular dependencies in relations.

### 1.2 Project Aggregate

Represents a unit of work and configuration.

- **Root**: `Project`
- **Components**:
  - `IssueType`: Custom issue types specific to the project.
  - `Workflow`: Workflows defined within the project scope.
- **Key Responsibilities**:
  - Generating unique Issue Keys (`PROJ-1`, `PROJ-2`).
  - Configuration of visibility and default roles.

### 1.3 Sprint Aggregate

A time-boxed planning entity.

- **Root**: `Sprint`
- **Key Responsibilities**:
  - managing lifecycle (`PLANNING` -> `ACTIVE` -> `COMPLETED`).
  - enforcing valid periods (Due date must be after Start date).

---

## 2. Invariants (Hard Rules)

These rules are enforced by the Domain Entities and cannot be violated.

### Issue Hierarchy

- **Parent-Child Constraint**:
  - A `SUBTASK` must have a `STANDARD` parent.
  - A `MICROTASK` must have a `SUBTASK` parent.
- **Workspace Consistency**: **Parent** and **Child** issues must belong to the same **Workspace**.
- **Project Consistency**: Generally, **Parent** and **Child** must be in the same **Project**.
  - (Exception: `EPIC` can be a parent of issues in other projects)

### Relations

- **Self-Reference**: An issue cannot link to itself.
- **Circular Dependency**: Relation chains cannot form a loop (e.g., A blocks B, B blocks A).
- **Workspace Boundary**: Relations are strictly limited to issues within the same Workspace.
  - Cross-Project relations are allowed.

### Workflow

- **Initial State**: There must be a single `INTIAL` category state and cannot be deleted.
- **Completion**: There must be at least one `COMPLETED` category state.
- **Guards**: Transitions can be conditionally blocked.
  - `NOT_BLOCKED`: The issue must not be blocked by other unresolved issues.
  - `REQUIRED_APPROVAL`: The issue must have a sufficient number of approved reviews.
- **Automation**: VCS events (e.g., PR Opened/Merged) can automatically trigger state transitions.

---

## 3. Policies (Soft Rules / Limits)

These are configurable constraints, often defined in `application.yml` or Policy classes.

### Member & Workspace

- **Max Workspaces**: A user can own/join a limited number of workspaces.
- **Max Members**: A workspace has a cap on the total number of members.
- **Withdrawal**: A Workspace Owner cannot withdraw (must transfer ownership first).

### Issue

- **Max Reviewers**: An issue typically limits reviewers (e.g., max 10).
- **Story Points**: Only specific hierarchy levels (e.g., `EPIC`, `STANDARD`) can have story points.

---

## 4. Security & Permissions (ACL)

Permissions are generally cascading:

1. **Workspace Role**: `OWNER` > `ADMIN` > `MEMBER`
2. **Project Role**: `ADMIN` > `MEMBER` > `VIEWER`

- **Author/Assignee Rights**: Even with low roles, the Author or Assignee of an issue usually has edit rights to that specific issue.
- **Elevated Access**: Sensitive operations (e.g., transferring ownership, changing emails) require re-authentication (Elevated Token).

---

## 5. Participation & Onboarding

This section describes how users enter the system and gain access to resources.

### 5.1 Member Identity
- **Multi-Identity Support**: A single `Member` can have multiple `AuthIdentities` (e.g., Email/Password + Google OAuth + GitHub OAuth).
- **Identity Linking**: An existing Member can link a new OAuth provider to their account after logging in.
- **Verification**: Email-based signup requires a verified `EmailVerificationToken`.

### 5.2 Workspace Participation
There are three primary ways to join a Workspace:
1. **Invitation**: An Admin sends an invitation to an email. The recipient must have a registered account to accept.
2. **Invite Link**: A unique, shareable link (Token). Can be set to expire or have a usage limit.
3. **Creation**: The creator of a workspace automatically becomes the `OWNER`.

### 5.3 Project Participation
- **Direct Join**: If a project visibility is `PUBLIC`, any Workspace Member can join directly without an invitation.
- **Auto-Join (Onboarding Config)**: When joining a Workspace via Invitation or Link, a user can be automatically added to specific projects defined in the `ProjectJoinConfig`.
- **Manual Addition**: Project Admins can add existing Workspace Members to the project manually.

### 5.4 Participation Invariants
- **Member Scope**: To join a Project, one MUST first be a member of the parent Workspace.
- **Role Inheritance**: Workspace `ADMIN` or `OWNER` can perform most actions in any project within that workspace, even if not explicitly a Project Member (Implicit Administrative Rights).
