from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.activity_log_response import ActivityLogResponse


# VIBE-CODED
# model: "claude-opus-4-8"
# evaluation: NOT_REVIEWED
class ActivityService:
    """Per-resource activity log reads."""

    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def list_issue_activities(
        self, issue_key: str, *, limit: int = 30
    ) -> list[ActivityLogResponse]:
        """The most recent activity entries for an issue (newest first).

        Only the first cursor page is fetched — enough for a recency timeline.
        """
        result = await self._client._call_with_retry(
            self._client.activity_log_api.list_issue_activities,
            issue_key,
            limit=limit,
        )
        return list(result.content or [])
