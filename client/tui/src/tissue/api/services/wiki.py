from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.create_document_request import CreateDocumentRequest
from tissue.api.generated.models.document_response import DocumentResponse
from tissue.api.generated.models.page_wiki_document_search_result import (
    PageWikiDocumentSearchResult,
)
from tissue.api.generated.models.set_document_parent_request import (
    SetDocumentParentRequest,
)
from tissue.api.generated.models.update_document_content_request import (
    UpdateDocumentContentRequest,
)
from tissue.api.generated.models.update_document_title_request import (
    UpdateDocumentTitleRequest,
)
from tissue.api.generated.models.wiki_document_detail import WikiDocumentDetail
from tissue.api.generated.models.wiki_document_tree_node import WikiDocumentTreeNode
from tissue.api.generated.models.wiki_snapshot_detail import WikiSnapshotDetail
from tissue.api.generated.models.wiki_snapshot_summary import WikiSnapshotSummary

if TYPE_CHECKING:
    from tissue.api.client import TissueClient


class WikiService:
    """Wiki read operations."""

    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def get_tree(self) -> list[WikiDocumentTreeNode]:
        """Flat list of every document (id, title, locked, parentDocumentId).

        The hierarchy tree is assembled using the parent pointers.
        """
        return await self._client._call_with_retry(
            self._client.wiki_document_api.get_wiki_document_tree,
        )

    async def get_document(self, wiki_id: int) -> WikiDocumentDetail:
        return await self._client._call_with_retry(
            self._client.wiki_document_api.get_wiki_document,
            wiki_id,
        )

    async def search(
        self,
        *,
        keyword: str | None = None,
        tag_ids: list[int] | None = None,
        page: int = 0,
        size: int = 20,
    ) -> PageWikiDocumentSearchResult:
        return await self._client._call_with_retry(
            self._client.wiki_document_api.search_wiki_documents,
            keyword=keyword,
            tag_ids=tag_ids,
            page=page,
            size=size,
        )

    async def create_document(
        self,
        *,
        title: str,
        content: str,
        parent_document_id: int | None = None,
    ) -> DocumentResponse:
        request = CreateDocumentRequest(
            title=title,
            content=content,
            parentDocumentId=parent_document_id,
        )
        return await self._client._call_with_retry(
            self._client.wiki_document_api.create_wiki_document,
            request,
        )

    async def update_title(self, wiki_id: int, *, title: str) -> None:
        request = UpdateDocumentTitleRequest(title=title)
        await self._client._call_with_retry(
            self._client.wiki_document_api.update_wiki_document_title,
            wiki_id,
            request,
        )

    async def update_content(
        self,
        wiki_id: int,
        *,
        content: str,
        version_update_type: str,
        edit_reason: str | None = None,
    ) -> None:
        request = UpdateDocumentContentRequest(
            content=content,
            versionUpdateType=version_update_type,
            editReason=edit_reason or None,
        )
        await self._client._call_with_retry(
            self._client.wiki_document_api.update_wiki_document_content,
            wiki_id,
            request,
        )

    async def set_parent(self, wiki_id: int, *, parent_document_id: int | None) -> None:
        request = SetDocumentParentRequest(parentDocumentId=parent_document_id)
        await self._client._call_with_retry(
            self._client.wiki_document_api.set_wiki_document_parent,
            wiki_id,
            request,
        )

    async def lock(self, wiki_id: int) -> None:
        await self._client._call_with_retry(
            self._client.wiki_document_api.lock_wiki_document,
            wiki_id,
        )

    async def unlock(self, wiki_id: int) -> None:
        await self._client._call_with_retry(
            self._client.wiki_document_api.unlock_wiki_document,
            wiki_id,
        )

    async def list_versions(self, wiki_id: int) -> list[WikiSnapshotSummary]:
        return await self._client._call_with_retry(
            self._client.wiki_document_api.list_wiki_document_versions,
            wiki_id,
        )

    async def get_version(self, wiki_id: int, snapshot_id: int) -> WikiSnapshotDetail:
        return await self._client._call_with_retry(
            self._client.wiki_document_api.get_wiki_document_version,
            wiki_id,
            snapshot_id,
        )
