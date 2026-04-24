from typing import Any

import httpx
import pydantic

from tissue.models.common import ErrorResponse


class TissueApiError(Exception):
    pass


class ApiNetworkError(TissueApiError):
    pass


class ApiResponseError(TissueApiError):
    def __init__(self, status_code: int, problem: ErrorResponse | None = None):
        self.status_code = status_code
        self.problem = problem
        parts = [f"HTTP {status_code}"]
        if problem:
            parts.append(f"[{problem.title}]")
        super().__init__(" ".join(parts))

    @property
    def code(self) -> str | None:
        return self.problem.title if self.problem else None

    @property
    def extras(self) -> dict[str, Any]:
        return self.problem.extras if self.problem else {}

    @classmethod
    def from_response(cls, resp: httpx.Response) -> "ApiResponseError":
        problem: ErrorResponse | None = None
        try:
            body = resp.json()
        except ValueError:
            body = None

        if isinstance(body, dict):
            try:
                problem = ErrorResponse.model_validate(body)
            except pydantic.ValidationError:
                problem = None
        return cls(resp.status_code, problem)


class ApiSchemaError(TissueApiError):
    pass
