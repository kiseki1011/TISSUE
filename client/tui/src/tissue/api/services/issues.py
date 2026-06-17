from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.page_response_issue_summary import (
    PageResponseIssueSummary,
)

if TYPE_CHECKING:
    from tissue.api.client import TissueClient


# VIBE-CODED
# model: "claude-opus-4-8"
# evaluation: NOT_REVIEWED
class IssueService:
    """Issue read operations."""

    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def search(
        self,
        *,
        keyword: str | None = None,
        page: int = 0,
        size: int = 20,
    ) -> PageResponseIssueSummary:
        """Full-text search across every project the caller is a member of.

        The generated `search_all_issues` takes the search fields as flat query
        params (the endpoint's `IssueSearchRequest` is a springdoc
        `@ParameterObject`), so we call it directly — no hand-rolled request.
        """
        return await self._client._call_with_retry(
            self._client.issue_api.search_all_issues,
            keyword=keyword,
            page=page,
            size=size,
        )
