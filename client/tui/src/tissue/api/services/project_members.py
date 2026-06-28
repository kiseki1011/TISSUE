from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.add_project_members_request import (
    AddProjectMembersRequest,
)
from tissue.api.generated.models.pageable import Pageable

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.member_candidate_summary import (
        MemberCandidateSummary,
    )
    from tissue.api.generated.models.project_member_response import (
        ProjectMemberResponse,
    )
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )


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
        pageable = Pageable(page=page, size=size, sort=None)
        result = await self._client._call_with_retry(
            self._client.project_member_api.list_project_members,
            project_key,
            pageable,
            role=role,
            keyword=keyword,
        )
        return list(result.content or [])

    async def list_member_candidates(
        self,
        project_key: str,
        *,
        keyword: str | None = None,
        page: int = 0,
        size: int = 30,
    ) -> list[MemberCandidateSummary]:
        pageable = Pageable(page=page, size=size, sort=None)
        result = await self._client._call_with_retry(
            self._client.project_member_api.list_member_candidates,
            project_key,
            pageable,
            keyword=keyword,
        )
        return list(result.content or [])

    async def add_project_members(
        self, project_key: str, member_ids: list[int]
    ) -> None:
        request = AddProjectMembersRequest(targetMemberIds=member_ids)
        await self._client._call_with_retry(
            self._client.project_member_api.add_project_members,
            project_key,
            request,
        )

    async def join_project(self, project_key: str) -> ProjectMemberResponse:
        return await self._client._call_with_retry(
            self._client.project_member_api.join_project,
            project_key,
        )
