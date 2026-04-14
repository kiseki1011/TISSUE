All `PATCH` requests follow the [RFC 7396](https://datatracker.ietf.org/doc/html/rfc7396) `application/merge-patch+json` format (a.k.a. JSON merge patch)

Each field in a patch request has three possible states:

| State           | JSON Representation        | Effect                |
|-----------------|----------------------------|-----------------------|
| Present         | `"fieldName": "new value"` | Update the field      |
| Explicit `null` | `"fieldName": null`        | Clear the field       |
| Absent          | *(field not in body)*      | Leave unchanged       |

## Example

Lets say there's an issue with the current values:

```json
{
  "title": "Resolve core service outage",
  "content": "Users in us-west-2 region cannot access server",
  "priority": "P1",
  "dueAt": "2025-01-10T00:00:00Z"
}
```

Sending this `PATCH` request:

```json
{
  "priority": "P0",
  "dueAt": null
}
```

Results in:

```json5
{
  "title": "Resolve core service outage", // Unchanged
  "content": "Users in us-west-2 region cannot access server", // Unchanged
  "priority": "P0", // Changed
  "dueAt": null // Cleared
}
```

## Constraints

- Required fields (ex: `name`, `title`) cannot be set to `null` or empty
- When a required field is provided, standard validation rules still apply
- Omitting a required field is valid (Like represented above, it means "Do not change")
