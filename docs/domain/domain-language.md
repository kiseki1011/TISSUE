# Domain Language

_This document defines the domain-specific language (ubiquitous language) used throughout Tissue.
It focuses on terminology and shared meaning to ensure consistency across code, UI, and documentation._

> [!WARNING]
> This project is under active development.
> The documentation can go through a massive change anytime.

---

## 1. Organization & Hierarchy

### Workspace

- The highest-level container. Represents a company or an organization.
- Identified by a globally unique **Workspace Key** (example: `TISSUE`)
- To join a workspace the user needs to be a signed up (Member) and join through accepting a invitation using a join link.

### Workspace Member

- A member that has joined the workspace
- A workspace member requires a **Workspace Role** which has a hierarchy of `OWNER` > `ADMIN` > `MEMBER`
- Identified with the **Workspace Key + Member ID**

### Project

- A distinct unit of work within a Workspace. Most work items and it's configurations are managed under this scope.
- Identified by a **Project Key** that is unique within the `Workspace` scope. Is 3-12 chars. (example: `BACK`, `FRONT`)
- **Visibility**:
  - `PUBLIC`: Can join without a project admin's permission.
  - `PRIVATE`: Must be invited or a project admin must put you in the project explicitly.

### Project Member

- A workspace member that joined the project.
- Identified with **Workspace Key + Project Key + Member ID**

### Team & Position

- **Team**: A functional group within a Workspace (example: "Frontend Team").
- **Position**: A job title or role description within a Workspace (example: "Senior Engineer").

---

## 2. Work Items

### Issue

- The main unit of work.
- Identified by an **Issue Key** (e.g., `BACK-101`).
- **Hierarchy Levels**:
  1. **EPIC**: High-level initiative (Parent of STANDARD).
  2. **STANDARD**: Regular task/story (Parent of SUBTASK).
  3. **SUBTASK**: Smaller breakdown of `STANDARD`.
  4. **MICROTASK**: Granular task.
- Issue has common fields and custom fields. Custom fields are implemented through issue field.

### Issue Type

- A custom defined type for a issue.
- A issue must have a issue type
- A issue type must have a workflow
- A issue type must have a issue hierarchy

### Issue Field

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
