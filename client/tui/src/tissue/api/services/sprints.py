from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.add_sprint_issues_request import (
    AddSprintIssuesRequest,
)
from tissue.api.generated.models.create_sprint_request import CreateSprintRequest
from tissue.api.generated.models.pageable import Pageable
from tissue.api.generated.models.remove_sprint_issues_request import (
    RemoveSprintIssuesRequest,
)

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

        `statuses` optionally narrows to a set of sprint statuses (PLANNING /
        ACTIVE / COMPLETED / CANCELLED); omitted means all.
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
        """Create a sprint in the project (PROJECT_MANAGER+). `title` is required
        (2-50 chars); `goal` is optional (<=255)."""
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
