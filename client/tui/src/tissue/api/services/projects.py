from __future__ import annotations

from typing import TYPE_CHECKING, Literal

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.create_project_request import CreateProjectRequest
from tissue.api.generated.models.page_project_summary import PageProjectSummary
from tissue.api.generated.models.pageable import Pageable
from tissue.api.generated.models.project_detail import ProjectDetail
from tissue.api.generated.models.project_response import ProjectResponse

if TYPE_CHECKING:
    from tissue.api.client import TissueClient

KeyAvailability = Literal["available", "taken", "reserved"]


class ProjectService:
    """Project domain operations.

    Authenticated calls go through `TissueClient._call_with_retry`, which
    refreshes the token once on a 401 and translates errors. Callers receive
    already-translated `TissueApiError`s.
    """

    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def list_projects(
        self,
        *,
        include_archived: bool = False,
        keyword: str | None = None,
        page: int = 0,
        size: int = 50,
        sort: list[str] | None = None,
    ) -> PageProjectSummary:
        pageable = Pageable(page=page, size=size, sort=sort)
        return await self._client._call_with_retry(
            self._client.project_api.list_projects,
            pageable,
            include_archived=include_archived,
            keyword=keyword,
        )

    async def get_project_detail(self, project_key: str) -> ProjectDetail:
        return await self._client._call_with_retry(
            self._client.project_api.get_project_detail,
            project_key,
        )

    async def archive_project(self, project_key: str) -> None:
        await self._client._call_with_retry(
            self._client.project_api.archive_project,
            project_key,
        )

    async def unarchive_project(self, project_key: str) -> None:
        await self._client._call_with_retry(
            self._client.project_api.unarchive_project,
            project_key,
        )

    async def create_project(
        self,
        *,
        project_key: str,
        title: str,
        description: str | None = None,
    ) -> ProjectResponse:
        request = CreateProjectRequest(
            projectKey=project_key,
            title=title,
            description=description or None,
        )
        return await self._client._call_with_retry(
            self._client.project_api.create_project,
            request,
        )

    async def check_project_key(self, project_key: str) -> KeyAvailability:
        """Availability check for a project key.

        Returns "available" / "taken" / "reserved". The 204 success maps to
        "available"; the server's 409 (duplicate) and 400 (reserved/invalid)
        map to "taken" / "reserved". Any other error propagates as a translated
        `TissueApiError`. Routes through `_call_with_retry`, so an expired token
        is refreshed once and retried.
        """
        try:
            await self._client._call_with_retry(
                self._client.project_api.check_project_key_availability,
                project_key,
            )
        except TissueApiError as e:
            if e.status == 409:
                return "taken"
            if e.status == 400:
                return "reserved"
            raise
        return "available"
