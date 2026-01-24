# Domain Language

_This document defines the domain-specific language (ubiquitous language) used throughout the Tissue project.
It focuses on terminology and shared meaning to ensure consistency across code, UI, and documentation._

---

## 1. Organization & Hierarchy

### Workspace

- The highest-level container. Represents a company or an organization.
- Identified by a unique **Workspace Key** (e.g., `TISSUE`).
- **Owner**: The creator of the workspace; has absolute control.
- **Invitation**: The mechanism to add members via Email or Invite Link.

### Project

- A distinct unit of work within a Workspace.
- Identified by a **Project Key** (3-12 chars, e.g., `BACK`, `FRONT`).
- **Visibility**:
  - `PUBLIC`: Visible to all workspace members.
  - `PRIVATE`: Visible only to invited project members.

### Team & Position

- **Team**: A functional group within a Workspace (e.g., "Frontend Team").
- **Position**: A job title or role description within a Workspace (e.g., "Senior Engineer").

---

## 2. Work Items (Issue)

### Issue

- The core unit of work.
- Identified by an **Issue Key**, combined from Project Key + Sequential Number (e.g., `BACK-101`).
- **Hierarchy Levels**:
  1. **EPIC**: High-level initiative (Parent of STANDARD).
  2. **STANDARD**: Regular task/story (Parent of SUBTASK).
  3. **SUBTASK**: Smaller breakdown of work.
  4. **MICROTASK**: Granular technical task.

### Issue Fields

- **Story Point**: An estimate of effort/complexity (Available for EPIC and STANDARD issues).
- **Custom Field**: User-defined data fields (Text, Number, Date, Enum, etc.) attached to specific Issue Types.

### Issue Participants

- **Reporter**: The person who reported the issue or is managing its lifecycle.
- **Assignee**: The person responsible for executing the work.
- **Reviewer**: A person requested to review the work.
- **Subscriber**: A person receiving notifications about updates.

### Relation

- The logical link between two issues.
- Types: `BLOCKS` (dependency), `DUPLICATES`, `RELEVANT`, `CAUSES`.

---

## 3. Workflow & Lifecycle

### Workflow

- Defines the lifecycle of an Issue through a directed graph of **States** and **Transitions**.
- **System Workflow**: Default workflows provided by the platform.

#### WorkflowState

- A specific stage in a workflow (e.g., "In Progress", "In Review").
- **State Category**: A system-level classification for every state:
  - `INITIAL`: The starting point (e.g., "To Do").
  - `ACTIVE`: Work in progress.
  - `COMPLETED`: Work finished (e.g., "Done", "Cancelled").

#### WorkflowTransition

- A valid movement from one State to another.
- **Guard**: A condition that must be met to execute a transition (e.g., `REQUIRED_APPROVAL`, `NOT_BLOCKED`).

### Sprint

- A time-boxed iteration of work within a Project.
- **Status**:
  - `PLANNING`: Issues are being collected.
  - `ACTIVE`: The sprint is currently running.
  - `COMPLETED`: The sprint has ended.

---

## 4. People & Roles

### Member

- A registered user account in the system (Global scope).
- identified by Email and Username.

### Workspace Member

- A Member's participation in a specific Workspace.
- **Roles**:
  - `OWNER`: Full control, billing, ownership transfer.
  - `ADMIN`: Manage workspace settings, members, and projects.
  - `MEMBER`: Standard access.

### Project Member

- A Workspace Member's participation in a specific Project.
- **Roles**:
  - `ADMIN`: Manage project settings, workflows, and attributes.
  - `MEMBER`: Create and edit issues.
  - `VIEWER`: Read-only access.

---

## 5. Integrations & Automation

### VCS (Version Control System)

- Integration with providers like GitHub/GitLab.
- **Connection**: Linking a Git Branch or Pull Request to an Issue.
- **Automation**: Moving an Issue's state automatically based on VCS events (e.g., "Move to DONE when PR Merged").

---

## 6. Identifier Formats

| Concept           | Format                                | Example                |
| :---------------- | :------------------------------------ | :--------------------- |
| **Workspace Key** | `WS-{Random Base64 String}` (8 chars) | `WS-A1B2C3D4`          |
| **Project Key**   | 3-12 Uppercase Alphanumeric           | `PAYMENT`, `IOS`       |
| **Issue Key**     | `{Project Key}-{Issue Number}`        | `PAYMENT-42`           |
| **Invite Link**   | UUID Token                            | `.../join/550e8400...` |
