All error responses follow the [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) `application/problem+json` format (a.k.a. Problem Details).

## Structure

| Field        | Type   | Description                                               |
|--------------|--------|-----------------------------------------------------------|
| `title`      | string | Application specific error code (ex: `VALIDATION_FAILED`) |
| `status`     | number | HTTP status code                                          |
| `detail`     | string | Explanation of the error                                  |
| `instance`   | string | The request path that caused the error                    |
| `occurredAt` | string | ISO 8601 timestamp of when the error occurred             |

Additional fields may be included depending on the error code.

## Examples

### Validation Error (400)

Returned when request body fields fail validation. Includes an `errors` map with per-field messages.

```json
{
  "type": "about:blank",
  "title": "VALIDATION_FAILED",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "instance": "/api/v1/members/signup",
  "occurredAt": "2025-01-05T12:00:00Z",
  "errors": {
    "username": "must not be blank",
    "password": "size must be between 8 and 100"
  }
}
```

### Resource Not Found (404)

```json
{
  "type": "about:blank",
  "title": "ISSUE_NOT_FOUND",
  "status": 404,
  "detail": "Issue not found",
  "instance": "/api/v1/issues/ETL-123/common",
  "occurredAt": "2025-01-05T12:00:00Z"
}
```

### Forbidden (403)

```json
{
  "type": "about:blank",
  "title": "PROJECT_MANAGER_REQUIRED",
  "status": 403,
  "detail": "Requires project manager role",
  "instance": "/api/v1/projects/ACME/members/456/role",
  "occurredAt": "2025-01-05T12:00:00Z"
}
```
