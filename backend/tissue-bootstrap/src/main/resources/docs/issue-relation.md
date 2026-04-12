Issues can be linked to each other using relations. Each relation has a type that defines its semantics.

## Relation Types

| Type         | Cycle Check | Description                                         |
|--------------|-------------|-----------------------------------------------------|
| `RELEVANT`   | No          | General reference between related issues             |
| `BLOCKS`     | Yes         | Source issue blocks the progress of the target issue |
| `CAUSES`     | Yes         | Source issue is the cause of the target issue        |
| `DUPLICATES` | Yes         | Source issue is a duplicate of the target issue      |

## Rules

- Both issues must belong to the **same workspace**
- An issue cannot make a relation with itself
- Only **one relation** can exist per source-target pair
- `BLOCKS`, `CAUSES`, and `DUPLICATES` relations must **not** form cycles
  - ex: A blocks B, B blocks A (A → B → A)
  - ex: A blocks B, B blocks C, C blocks A (A → B → C → A)
