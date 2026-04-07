Issues follow a multi level hierarchy that determines parent-child relationships.

## Hierarchy Levels

| Level | Type         | Description                    |
|-------|--------------|--------------------------------|
| 1     | `EPIC`       | Highest level grouping of work |
| 2     | `STANDARD`   | Regular issue                  |
| 3     | `SUBTASK`    | Sub-unit of a standard issue   |
| 4     | `MICROTASK`  | Smallest unit of work          |

## Parent-Child Rules

- A parent must be **exactly one level above** the child (ex: `EPIC` → `STANDARD`, `STANDARD` → `SUBTASK`)
- An issue **cannot be its own parent**
- Parent and child must belong to the **same workspace**
- `SUBTASK` and `MICROTASK` can only have parents within the **same project**
- `STANDARD` issues allow **cross-project parents** (ex: an EPIC in project A can parent a STANDARD in project B)
- `SUBTASK` and `MICROTASK` **always require a parent** and cannot be orphaned
- `EPIC` and `STANDARD` can exist without a parent

## Story Points

- Only `STANDARD` issues can modify story points directly
- `EPIC` and `STANDARD` issues can display story points
- The story point of `EPIC` is automatically calculated using the sum of its child issues
