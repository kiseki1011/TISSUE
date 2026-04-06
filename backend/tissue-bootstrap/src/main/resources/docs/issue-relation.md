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
- `BLOCKS`, `CAUSES`, and `DUPLICATES` relations **must not form cycles** 
  - ex1: A blocks B, B blocks A
  - ex2: Blocking relation chain like A → B → C → A
- `RELEVANT` relations are exempt from cycle detection
- `DUPLICATES` relations require both issues to have the **same issue type**
