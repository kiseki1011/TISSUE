from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.pageable import Pageable

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.page_response_sprint_summary import (
        PageResponseSprintSummary,
    )
    from tissue.api.generated.models.sprint_detail import SprintDetail


class SprintService:
    """Sprint read operations for the project hub."""

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
