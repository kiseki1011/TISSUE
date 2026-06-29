from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING

from tissue.api.generated.models.add_issue_relation_request import (
    AddIssueRelationRequest,
)
from tissue.api.generated.models.assign_parent_issue_request import (
    AssignParentIssueRequest,
)
from tissue.api.generated.models.batch_change_parent_request import (
    BatchChangeParentRequest,
)
from tissue.api.generated.models.create_issue_request import CreateIssueRequest
from tissue.api.generated.models.page_response_issue_summary import (
    PageResponseIssueSummary,
)
from tissue.api.generated.models.perform_transition_request import (
    PerformTransitionRequest,
)
from tissue.api.generated.models.remove_issue_relation_request import (
    RemoveIssueRelationRequest,
)
from tissue.api.generated.models.request_review_request import RequestReviewRequest
from tissue.api.generated.models.submit_review_request import SubmitReviewRequest
from tissue.api.generated.models.update_custom_fields_request import (
    UpdateCustomFieldsRequest,
)
from tissue.api.generated.models.update_story_point_request import (
    UpdateStoryPointRequest,
)

if TYPE_CHECKING:
    from typing import Any

    from tissue.api.client import TissueClient
    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.batch_operation_response import (
        BatchOperationResponse,
    )
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_detail_view import IssueDetailView
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )
    from tissue.api.generated.models.issue_relations_detail import (
        IssueRelationsDetail,
    )
    from tissue.api.generated.models.issue_type_detail import IssueTypeDetail
    from tissue.api.generated.models.issue_type_summary import IssueTypeSummary


class IssueService:
    """Read and write operations for issues."""

    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def search(
        self,
        *,
        keyword: str | None = None,
        page: int = 0,
        size: int = 20,
    ) -> PageResponseIssueSummary:
        return await self._client._call_with_retry(
            self._client.issue_api.search_all_issues,
            keyword=keyword,
            page=page,
            size=size,
        )

    async def my_work(
        self,
        *,
        page: int = 0,
        size: int = 20,
    ) -> PageResponseIssueSummary:
        return await self._client._call_with_retry(
            self._client.issue_api.search_all_issues,
            assignee_member_ids=["me"],
            page=page,
            size=size,
        )

    async def search_project_issues(
        self,
        project_key: str,
        *,
        keyword: str | None = None,
        sprint_ids: list[int] | None = None,
        assignee_member_ids: list[str] | None = None,
        reviewer_member_ids: list[str] | None = None,
        reviewer_statuses: list[str] | None = None,
        state_categories: list[str] | None = None,
        priorities: list[str] | None = None,
        current_sprint_only: bool | None = None,
        page: int = 0,
        size: int = 50,
    ) -> PageResponseIssueSummary:
        return await self._client._call_with_retry(
            self._client.issue_api.search_project_issues,
            project_key=project_key,
            keyword=keyword,
            sprint_ids=sprint_ids,
            assignee_member_ids=assignee_member_ids,
            reviewer_member_ids=reviewer_member_ids,
            reviewer_statuses=reviewer_statuses,
            state_categories=state_categories,
            priorities=priorities,
            current_sprint_only=current_sprint_only,
            page=page,
            size=size,
        )

    async def get_issue(self, issue_key: str) -> IssueCommonDetail:
        return await self._client._call_with_retry(
            self._client.issue_api.get_issue_common,
            issue_key=issue_key,
        )

    async def get_issue_detail(self, issue_key: str) -> IssueDetailView:
        return await self._client._call_with_retry(
            self._client.issue_api.get_issue_detail_view,
            issue_key=issue_key,
        )

    async def get_issue_custom_fields(
        self, issue_key: str
    ) -> list[CustomFieldValueInfo]:
        detail = await self._client._call_with_retry(
            self._client.issue_api.get_issue_custom,
            issue_key=issue_key,
        )
        return list(detail.custom_fields or [])

    async def get_issue_type(self, issue_type_id: int) -> IssueTypeDetail:
        return await self._client._call_with_retry(
            self._client.custom_issue_type_api.get_issue_type,
            issue_type_id=issue_type_id,
        )

    async def list_issue_types(self) -> list[IssueTypeSummary]:
        return await self._client._call_with_retry(
            self._client.custom_issue_type_api.list_issue_types,
        )

    async def create_issue(
        self,
        project_key: str,
        *,
        issue_type_id: int,
        title: str,
        priority: str,
        content: str | None = None,
        summary: str | None = None,
        assignee_member_id: int | None = None,
        story_point: int | None = None,
        due_at: str | None = None,
        custom_fields: dict[str, Any] | None = None,
        parent_issue_key: str | None = None,
    ) -> str | None:
        request = CreateIssueRequest(
            issueTypeId=issue_type_id,
            title=title,
            priority=priority,
            content=content or None,
            summary=summary or None,
            assigneeMemberId=assignee_member_id,
            storyPoint=story_point,
            dueAt=datetime.fromisoformat(due_at) if due_at else None,
            customFields=custom_fields or None,
            parentIssueKey=parent_issue_key,
        )
        response = await self._client._call_with_retry(
            self._client.issue_api.create_issue,
            project_key=project_key,
            create_issue_request=request,
        )
        return response.issue_key

    async def update_custom_fields(
        self, issue_key: str, custom_fields: dict[str, Any]
    ) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.update_issue_custom_fields,
            issue_key=issue_key,
            update_custom_fields_request=UpdateCustomFieldsRequest(
                customFields=custom_fields
            ),
        )

    async def get_transitions(self, issue_key: str) -> list[AvailableTransition]:
        return await self._client._call_with_retry(
            self._client.issue_api.get_issue_available_transitions,
            issue_key=issue_key,
        )

    async def perform_transition(self, issue_key: str, transition_id: int) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.perform_issue_transition,
            issue_key=issue_key,
            perform_transition_request=PerformTransitionRequest(
                transitionId=transition_id
            ),
        )

    async def assign_issue(self, issue_key: str, member_id: int) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.assign_issue,
            issue_key=issue_key,
            member_id=member_id,
        )

    async def unassign_issue(self, issue_key: str) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.unassign_issue,
            issue_key=issue_key,
        )

    async def add_reviewer(self, issue_key: str, member_id: int) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.add_issue_reviewer,
            issue_key=issue_key,
            target_member_id=member_id,
        )

    async def remove_reviewer(self, issue_key: str, member_id: int) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.remove_issue_reviewer,
            issue_key=issue_key,
            target_member_id=member_id,
        )

    async def request_review(self, issue_key: str, member_ids: list[int]) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.request_issue_review,
            issue_key=issue_key,
            request_review_request=RequestReviewRequest(reviewerMemberIds=member_ids),
        )

    async def submit_review(self, issue_key: str, *, approved: bool) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.submit_issue_review,
            issue_key=issue_key,
            submit_review_request=SubmitReviewRequest(approved=approved),
        )

    async def update_common_fields(
        self,
        issue_key: str,
        *,
        title: str | None = None,
        summary: str | None = None,
        content: str | None = None,
        priority: str | None = None,
        due_at: str | None = None,
        clear_due_at: bool = False,
    ) -> None:
        """Partial update of an issue's common fields.

        The generated client cannot model the backend's `JsonNullable<T>` fields:
        it produces `{present: bool}` without a value slot. This method builds the
        raw JSON body the server expects and sends it through `_patch_common_fields`.
        `clear_due_at` sends an explicit null.
        """
        body = _common_field_patch_body(
            title=title,
            summary=summary,
            content=content,
            priority=priority,
            due_at=due_at,
            clear_due_at=clear_due_at,
        )
        if not body:
            return
        await self._client._call_with_retry(self._patch_common_fields, issue_key, body)

    async def _patch_common_fields(
        self, issue_key: str, body: dict[str, object | None]
    ) -> None:
        await self._client._send_raw_patch(
            self._client.issue_api._update_issue_common_fields_serialize,
            issue_key=issue_key,
            update_common_fields_request=body,
            response_types_map={"204": None, "400": None, "404": None},
        )

    async def get_issue_parent(self, issue_key: str) -> IssueIdentifierResponse:
        return await self._client._call_with_retry(
            self._client.issue_api.get_issue_parent,
            issue_key=issue_key,
        )

    async def get_issue_children(self, issue_key: str) -> list[IssueIdentifierResponse]:
        return await self._client._call_with_retry(
            self._client.issue_api.get_issue_children,
            issue_key=issue_key,
        )

    async def assign_parent(self, issue_key: str, parent_issue_key: str) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.assign_issue_parent,
            issue_key=issue_key,
            assign_parent_issue_request=AssignParentIssueRequest(
                parentIssueKey=parent_issue_key
            ),
        )

    async def remove_parent(self, issue_key: str) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.remove_issue_parent,
            issue_key=issue_key,
        )

    async def add_children(
        self, project_key: str, parent_issue_key: str, child_issue_keys: list[str]
    ) -> BatchOperationResponse:
        return await self._client._call_with_retry(
            self._client.issue_api.batch_change_issue_parent,
            project_key=project_key,
            batch_change_parent_request=BatchChangeParentRequest(
                issueKeys=child_issue_keys, parentIssueKey=parent_issue_key
            ),
        )

    async def get_issue_relations(self, issue_key: str) -> IssueRelationsDetail:
        return await self._client._call_with_retry(
            self._client.issue_api.get_issue_relations,
            issue_key=issue_key,
        )

    async def add_issue_relation(
        self,
        issue_key: str,
        target_project_key: str,
        target_issue_key: str,
        relation_type: str,
    ) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.add_issue_relation,
            issue_key=issue_key,
            add_issue_relation_request=AddIssueRelationRequest(
                relationType=relation_type,
                targetIssueKey=target_issue_key,
                targetProjectKey=target_project_key,
            ),
        )

    async def remove_issue_relation(
        self, issue_key: str, target_project_key: str, target_issue_key: str
    ) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.remove_issue_relation,
            issue_key=issue_key,
            remove_issue_relation_request=RemoveIssueRelationRequest(
                targetIssueKey=target_issue_key,
                targetProjectKey=target_project_key,
            ),
        )

    async def update_story_point(self, issue_key: str, story_point: int | None) -> None:
        await self._client._call_with_retry(
            self._client.issue_api.update_issue_story_point,
            issue_key=issue_key,
            update_story_point_request=UpdateStoryPointRequest(storyPoint=story_point),
        )


def _common_field_patch_body(
    *,
    title: str | None,
    summary: str | None,
    content: str | None,
    priority: str | None,
    due_at: str | None,
    clear_due_at: bool,
) -> dict[str, object | None]:
    body: dict[str, object | None] = {}
    if title is not None:
        body["title"] = title
    if summary is not None:
        body["summary"] = summary
    if content is not None:
        body["content"] = content
    if priority is not None:
        body["priority"] = priority
    if clear_due_at:
        body["dueAt"] = None
    elif due_at is not None:
        body["dueAt"] = due_at
    return body
