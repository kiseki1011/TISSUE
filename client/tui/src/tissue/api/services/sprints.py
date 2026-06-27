from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING

from tissue.api.generated.models.add_sprint_issues_request import (
    AddSprintIssuesRequest,
)
from tissue.api.generated.models.create_sprint_request import CreateSprintRequest
from tissue.api.generated.models.pageable import Pageable
from tissue.api.generated.models.remove_sprint_issues_request import (
    RemoveSprintIssuesRequest,
)
from tissue.api.generated.models.start_sprint_request import StartSprintRequest

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.page_response_sprint_summary import (
        PageResponseSprintSummary,
    )
    from tissue.api.generated.models.sprint_command_result import SprintCommandResult
    from tissue.api.generated.models.sprint_detail import SprintDetail


class SprintService:
    """Sprint read and command operations for the project hub."""

    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def list_project_sprints(
        self,
        project_key: str,
        *,
        statuses: list[str] | None = None,
        page: int = 0,
        size: int = 50,
    ) -> PageResponseSprintSummary:
        """Sprints of a single project, for the hub's Sprints tab.

        `statuses` optionally narrows to a set of sprint statuses, omitted
        means all.

        Sprint status:
            - PLANNING
            - ACTIVE
            - COMPLETED
            - CANCELLED
        """
        pageable = Pageable(page=page, size=size, sort=None)
        return await self._client._call_with_retry(
            self._client.sprint_api.list_project_sprints,
            project_key,
            pageable,
            statuses=statuses,
        )

    async def get_sprint(self, sprint_id: int) -> SprintDetail:
        """A single sprint's detail (goal + lifecycle timestamps)."""
        return await self._client._call_with_retry(
            self._client.sprint_api.get_sprint,
            sprint_id,
        )

    async def create_sprint(
        self, project_key: str, *, title: str, goal: str | None = None
    ) -> SprintCommandResult:
        """Create a sprint in the project, requires PROJECT_MANAGER or above.

        `title` is required (2-50 chars), `goal` is optional (<=255).
        """
        request = CreateSprintRequest(title=title, goal=goal)
        return await self._client._call_with_retry(
            self._client.sprint_api.create_sprint,
            project_key,
            request,
        )

    async def add_sprint_issues(self, sprint_id: int, issue_keys: list[str]) -> None:
        """Assign issues to a sprint (bulk, by issue key)."""
        request = AddSprintIssuesRequest(issueKeys=issue_keys)
        await self._client._call_with_retry(
            self._client.sprint_api.add_sprint_issues,
            sprint_id,
            request,
        )

    async def remove_sprint_issues(self, sprint_id: int, issue_keys: list[str]) -> None:
        """Remove issues from a sprint (bulk, by issue key)."""
        request = RemoveSprintIssuesRequest(issueKeys=issue_keys)
        await self._client._call_with_retry(
            self._client.sprint_api.remove_sprint_issues,
            sprint_id,
            request,
        )

    async def start_sprint(self, sprint_id: int, *, due_at: str) -> None:
        """PLANNING -> ACTIVE. The backend sets started_at to now; due_at is required.

        `due_at` is a UTC ISO-8601 instant string.
        """
        request = StartSprintRequest(dueAt=datetime.fromisoformat(due_at))
        await self._client._call_with_retry(
            self._client.sprint_api.start_sprint,
            sprint_id,
            request,
        )

    async def complete_sprint(self, sprint_id: int) -> None:
        """ACTIVE -> COMPLETED. Rejected if any sprint issue is unfinished."""
        await self._client._call_with_retry(
            self._client.sprint_api.complete_sprint,
            sprint_id,
        )

    async def cancel_sprint(self, sprint_id: int) -> None:
        """PLANNING/ACTIVE -> CANCELLED. Unassigns the sprint's issues."""
        await self._client._call_with_retry(
            self._client.sprint_api.cancel_sprint,
            sprint_id,
        )

    async def update_sprint(
        self,
        sprint_id: int,
        *,
        title: str | None = None,
        goal: str | None = None,
        started_at: str | None = None,
        due_at: str | None = None,
    ) -> None:
        """Partial update of a sprint's mutable fields (only provided ones are sent).

        Same JsonNullable raw-dict workaround as `IssueService.update_common_fields`:
        the backend wraps these in `JsonNullable<T>`, which the generator mis-modeled,
        so we send the raw body the server expects instead of the broken model.
        """
        body: dict[str, object | None] = {}
        if title is not None:
            body["title"] = title
        if goal is not None:
            body["goal"] = goal
        if started_at is not None:
            body["startedAt"] = started_at
        if due_at is not None:
            body["dueAt"] = due_at
        if not body:
            return
        await self._client._call_with_retry(self._patch_sprint, sprint_id, body)

    async def _patch_sprint(
        self, sprint_id: int, body: dict[str, object | None]
    ) -> None:
        """Low-level PATCH that sends a raw dict body (see `update_sprint`)."""
        api = self._client.sprint_api
        param = api._update_sprint_serialize(
            sprint_id=sprint_id,
            update_sprint_request=body,  # pyright: ignore[reportArgumentType]
            _request_auth=None,
            _content_type=None,
            _headers=None,
            _host_index=0,
        )
        response = await api.api_client.call_api(*param)
        await response.read()
