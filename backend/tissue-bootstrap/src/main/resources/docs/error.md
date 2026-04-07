All error responses follow the [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) `application/problem+json` format (a.k.a. Problem Details).

## Structure

| Field        | Type   | Description                                               |
|--------------|--------|-----------------------------------------------------------|
| `title`      | string | Application-specific error code (ex: `VALIDATION_FAILED`) |
| `status`     | number | HTTP status code                                          |
| `detail`     | string | Explanation of the error                                  |
| `instance`   | string | The request path that caused the error                    |
| `occurredAt` | string | ISO 8601 timestamp of when the error occurred             |

Additional fields may be included depending on the error type.

## Example

```json
{
  "type": "about:blank",
  "title": "VALIDATION_FAILED",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "instance": "/api/v1/members/signup",
  "occurredAt": "2026-01-05T12:00:00Z",
  "errors": {
    "username": "must not be blank",
    "password": "size must be between 8 and 100"
  }
}
```
