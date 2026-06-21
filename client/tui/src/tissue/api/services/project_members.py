from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.pageable import Pageable

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
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
        """All members of a project (flattened from the paged response).

        A single page of `size` is fine for small/mid teams; callers use the
        result to resolve member ids to names and to populate member pickers.
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
