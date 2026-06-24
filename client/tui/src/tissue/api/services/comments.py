from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.add_comment_request import AddCommentRequest
from tissue.api.generated.models.pageable import Pageable

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.comment_create_response import (
        CommentCreateResponse,
    )
    from tissue.api.generated.models.comment_detail_response import (
        CommentDetailResponse,
    )


# VIBE-CODED
# model: "claude-opus-4-8"
# evaluation: NOT_REVIEWED
class CommentService:
    """Issue comment read/write operations."""

    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def list_issue_comments(
        self, issue_key: str, *, page: int = 0, size: int = 50
    ) -> list[CommentDetailResponse]:
        """Root comments on an issue (each carries its replies nested, depth 1)."""
        pageable = Pageable(page=page, size=size, sort=None)
        result = await self._client._call_with_retry(
            self._client.comment_api.list_issue_comments,
            issue_key,
            pageable,
        )
        return list(result.content or [])

    async def create_comment(
        self,
        issue_key: str,
        content: str,
        *,
        parent_comment_id: int | None = None,
        mentioned_usernames: list[str] | None = None,
    ) -> CommentCreateResponse:
        """Add a comment to an issue — a reply when `parent_comment_id` is given,
        otherwise a root comment. The backend caps nesting at one level.
        `mentioned_usernames` are the @-mentioned members the server notifies — it
        does not parse the body, so this explicit list is what drives the mention
        notifications."""
        return await self._client._call_with_retry(
            self._client.comment_api.create_comment,
            issue_key,
            AddCommentRequest(
                content=content,
                parentCommentId=parent_comment_id,
                mentionedUsernames=mentioned_usernames,
            ),
        )
