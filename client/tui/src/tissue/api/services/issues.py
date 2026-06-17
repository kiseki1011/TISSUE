from __future__ import annotations

import json
from typing import TYPE_CHECKING

from tissue.api.generated.models.page_issue_summary import PageIssueSummary

if TYPE_CHECKING:
    from tissue.api.client import TissueClient

_SEARCH_PATH = "/api/v1/issues:search"


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
    ) -> PageIssueSummary:
        """Full-text search across every project the caller is a member of."""
        return await self._client._call_with_retry(
            self._fetch_search, keyword, page, size
        )

    async def _fetch_search(
        self, keyword: str | None, page: int, size: int
    ) -> PageIssueSummary:
        api_client = self._client._api_client
        query: list[tuple[str, object]] = []
        if keyword:
            query.append(("keyword", keyword))
        query.append(("page", page))
        query.append(("size", size))

        params = api_client.param_serialize(
            method="GET",
            resource_path=_SEARCH_PATH,
            query_params=query,
            header_params={"Accept": "application/json"},
            auth_settings=["bearerAuth"],
        )
        response = await api_client.call_api(*params)
        await response.read()

        api_client.response_deserialize(
            response_data=response, response_types_map={"200": None}
        )

        raw = response.data
        data = json.loads(raw.decode("utf-8")) if raw else {}

        if not isinstance(data.get("pageable"), dict):
            data["pageable"] = None
        return PageIssueSummary.from_dict(data) or PageIssueSummary(content=[])
