from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.pageable import Pageable

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.project_member_response import (
        ProjectMemberResponse,
    )
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )


# VIBE-CODED
# model: "claude-opus-4-8"
# evaluation: NOT_REVIEWED
class ProjectMemberService:
    """Project membership operations."""

    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def list_project_members(
        self,
        project_key: str,
        *,
        keyword: str | None = None,
        role: str | None = None,
        page: int = 0,
        size: int = 100,
    ) -> list[ProjectMemberSummary]:
        """All members of a project, flattened from the paged response.

        A single page of `size` is fine for small or mid teams. Callers use the
        result to look up member ids to names and to populate member pickers.
        """
        pageable = Pageable(page=page, size=size, sort=None)
        result = await self._client._call_with_retry(
            self._client.project_member_api.list_project_members,
            project_key,
            pageable,
            role=role,
            keyword=keyword,
        )
        return list(result.content or [])

    async def join_project(self, project_key: str) -> ProjectMemberResponse:
        """Join a project as the current user.

        For an existing member the server returns the membership unchanged.
        Raises `TissueApiError` (403) when joining isn't allowed, such as a
        PRIVATE project the user isn't already a member of.
        """
        return await self._client._call_with_retry(
            self._client.project_member_api.join_project,
            project_key,
        )
