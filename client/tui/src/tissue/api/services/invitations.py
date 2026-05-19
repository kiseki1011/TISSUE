from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.invitation_detail import InvitationDetail

if TYPE_CHECKING:
    from tissue.api.client import TissueClient


class InvitationService:
    def __init__(self, client: TissueClient) -> None:
        self._client = client
        self._invitations: list[InvitationDetail] | None = None

    @property
    def cached(self) -> list[InvitationDetail] | None:
        return self._invitations

    def _set_cached(self, invitations: list[InvitationDetail] | None) -> None:
        """Set by client during prefetch. Clear on logout."""
        self._invitations = invitations

    async def accept(self, invitation_id: int) -> None:
        """Accept an invitation.

        Refreshes invitations and workspaces caches since the user is now a
        member of the workspace.
        """
        await self._client._call_with_retry(
            self._client.invitation_api.accept_invitation, invitation_id
        )
        await self.refresh()
        await self._client.workspaces.refresh()

    async def reject(self, invitation_id: int) -> None:
        await self._client._call_with_retry(
            self._client.invitation_api.reject_invitation, invitation_id
        )
        await self.refresh()

    async def refresh(self) -> None:
        """Refresh the user's invitation list and replace the cache."""
        self._invitations = await self._client._call_with_retry(
            self._client.invitation_api.list_my_invitations
        )
