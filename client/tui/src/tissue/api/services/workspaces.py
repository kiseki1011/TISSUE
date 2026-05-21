from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.create_workspace_request import (
    CreateWorkspaceRequest,
)
from tissue.api.generated.models.invite_members_response import InviteMembersResponse
from tissue.api.generated.models.invite_to_workspace_request import (
    InviteToWorkspaceRequest,
)
from tissue.api.generated.models.workspace_create_response import (
    WorkspaceCreateResponse,
)
from tissue.api.generated.models.workspace_summary_response import (
    WorkspaceSummaryResponse,
)

if TYPE_CHECKING:
    from tissue.api.client import TissueClient


class WorkspaceService:
    def __init__(self, client: TissueClient) -> None:
        self._client = client
        self._workspaces: list[WorkspaceSummaryResponse] | None = None

    @property
    def cached(self) -> list[WorkspaceSummaryResponse] | None:
        return self._workspaces

    def _set_cached(self, workspaces: list[WorkspaceSummaryResponse] | None) -> None:
        """Set by client during prefetch. Clear on logout."""
        self._workspaces = workspaces

    async def create(
        self,
        workspace_key: str,
        name: str,
        description: str | None = None,
    ) -> WorkspaceCreateResponse:
        """Create a workspace and refresh the cached workspace list."""
        request = CreateWorkspaceRequest(
            workspaceKey=workspace_key,
            name=name,
            description=description,
        )
        response = await self._client._call_with_retry(
            self._client.workspace_api.create_workspace, request
        )
        await self.refresh()
        return response

    async def invite(
        self,
        workspace_key: str,
        emails: list[str],
        role: str = "MEMBER",
    ) -> InviteMembersResponse:
        request = InviteToWorkspaceRequest(emails=emails, role=role)
        return await self._client._call_with_retry(
            self._client.workspace_participation_api.invite_to_workspace,
            workspace_key,
            request,
        )

    async def refresh(self) -> None:
        """Refresh the user's workspace list and replace the cache."""
        self._workspaces = await self._client._call_with_retry(
            self._client.workspace_api.list_my_workspaces
        )

    async def check_key_available(self, key: str) -> bool:
        """Return True if the workspace key is available, False if already taken.

        `_call_with_retry` already translates ApiException/HTTPError into
        TissueApiError and re-raises it, so we only need to catch the
        translated form here (catching the raw ones would never match).
        """
        try:
            await self._client._call_with_retry(
                self._client.workspace_api.check_workspace_key_availability, key
            )
            return True
        except TissueApiError as err:
            if err.status == 409:
                return False
            raise
