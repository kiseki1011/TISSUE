import logging

import httpx
import pydantic

from tissue.api.generated.exceptions import ApiException
from tissue.models.common import ErrorResponse

log = logging.getLogger(__name__)


class TissueApiError(Exception):
    def __init__(
        self,
        message: str,
        *,
        problem: ErrorResponse | None = None,
        status: int | None = None,
    ) -> None:
        super().__init__(message)
        self.problem = problem
        self.title = problem.title if problem else None
        if status is not None:
            self.status = status
        else:
            self.status = problem.status if problem else None
        self.detail = problem.detail if problem else None

    def __str__(self) -> str:
        tags = ""
        if self.status is not None:
            tags += f"[{self.status}]"
        if self.title:
            tags += f"[{self.title}]"
        base = super().__str__()
        return f"{tags} {base}" if tags else base


class ConnectionFailed(TissueApiError):
    pass


class NotTissueServer(TissueApiError):
    pass


class InvalidCredentials(TissueApiError):
    pass


class ServerError(TissueApiError):
    pass


def translate(exc: Exception) -> TissueApiError:
    if isinstance(exc, httpx.ConnectError | httpx.ConnectTimeout):
        return ConnectionFailed("Cannot reach server")

    if isinstance(exc, httpx.TimeoutException):
        return ConnectionFailed("Server timeout")

    if isinstance(exc, httpx.HTTPError):
        return ConnectionFailed(f"Network error: {exc}")

    if isinstance(exc, ApiException):
        problem = _parse_problem(exc.body)
        return _from_problem(exc.status, problem)

    return TissueApiError(str(exc))


# TODO: separate to error_mappings.py if it gets bigger
_TITLE_TO_CLASS: dict[str, type[TissueApiError]] = {
    "INVALID_CREDENTIALS": InvalidCredentials,
}


def _parse_problem(body: str | None) -> ErrorResponse | None:
    if not body:
        return None
    try:
        return ErrorResponse.model_validate_json(body)
    except (pydantic.ValidationError, ValueError) as e:
        log.warning("Failed to parse problem details: %s", e)
        return None


def _from_problem(status: int | None, problem: ErrorResponse | None) -> TissueApiError:
    if problem and problem.title in _TITLE_TO_CLASS:
        cls = _TITLE_TO_CLASS[problem.title]
        return cls(problem.detail or problem.title, problem=problem)

    if status == 401:
        return InvalidCredentials(
            "Authentication required", problem=problem, status=status
        )

    if status and status >= 500:
        return ServerError(f"Server error: {status}", problem=problem, status=status)

    message = problem.detail if problem else f"API error: {status}"
    return TissueApiError(message or "Unknown error", problem=problem, status=status)
