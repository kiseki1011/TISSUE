from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING

from tissue.api.generated.models.create_issue_request import CreateIssueRequest
from tissue.api.generated.models.page_response_issue_summary import (
    PageResponseIssueSummary,
)
from tissue.api.generated.models.perform_transition_request import (
    PerformTransitionRequest,
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
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_type_detail import IssueTypeDetail
    from tissue.api.generated.models.issue_type_summary import IssueTypeSummary


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
        """Full-text search across every project the caller is a member of."""
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
        """Issues assigned to the current user, across every project they belong to."""
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
        """Issues within a single project, for the project hub.

        Combines a keyword search with optional filters: `sprint_ids` narrows to
        issues belonging to those sprints (a sprint's issues), `current_sprint_only`
        folds in the project's active sprint, `assignee_member_ids` to those assigned
        to any of the given members (or "me"), `reviewer_member_ids` to those the
        given members review, `reviewer_statuses` further narrows that reviewer match
        to reviews in one of those statuses (PENDING / APPROVED / CHANGES_REQUESTED —
        only meaningful with `reviewer_member_ids`), `state_categories` to a workflow
        category such as INITIAL / ACTIVE / COMPLETED / ABORTED, and `priorities` to
        issue priorities (P0-P4). The member/sprint lists are OR-matched.
        """
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
        """The common (read) fields of a single issue."""
        return await self._client._call_with_retry(
            self._client.issue_api.get_issue_common,
            issue_key=issue_key,
        )

    async def get_issue_custom_fields(
        self, issue_key: str
    ) -> list[CustomFieldValueInfo]:
        """The issue's custom field values (the fields its issue type defines)."""
        detail = await self._client._call_with_retry(
            self._client.issue_api.get_issue_custom,
            issue_key=issue_key,
        )
        return list(detail.custom_fields or [])

    async def get_issue_type(self, issue_type_id: int) -> IssueTypeDetail:
        """An issue type's full definition, including its custom field
        definitions and their selectable options (for SELECT_OPTION/CHECKLIST)."""
        return await self._client._call_with_retry(
            self._client.custom_issue_type_api.get_issue_type,
            issue_type_id=issue_type_id,
        )

    async def list_issue_types(self) -> list[IssueTypeSummary]:
        """All global issue types (id + name + workflow), for the create form's
        type picker. Use `get_issue_type` for a type's full field definitions."""
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
    ) -> str | None:
        """Create an issue in `project_key`, returning its new key (or None).

        `due_at` is an ISO-8601 instant string (parsed to a datetime for the
        request); `custom_fields` maps a field id (string key) to its value,
        shaped per the field's type (see `widgets.custom_field_input`)."""
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
        """Update one or more custom field values. `custom_fields` maps a field id
        (as a string key) to its new value, shaped per the field's type."""
        await self._client._call_with_retry(
            self._client.issue_api.update_issue_custom_fields,
            issue_key=issue_key,
            update_custom_fields_request=UpdateCustomFieldsRequest(
                customFields=custom_fields
            ),
        )

    async def get_transitions(self, issue_key: str) -> list[AvailableTransition]:
        """Workflow transitions available from the issue's current state."""
        return await self._client._call_with_retry(
            self._client.issue_api.get_issue_available_transitions,
            issue_key=issue_key,
        )

    async def perform_transition(self, issue_key: str, transition_id: int) -> None:
        """Execute a workflow transition, moving the issue to a new state."""
        await self._client._call_with_retry(
            self._client.issue_api.perform_issue_transition,
            issue_key=issue_key,
            perform_transition_request=PerformTransitionRequest(
                transitionId=transition_id
            ),
        )

    async def assign_issue(self, issue_key: str, member_id: int) -> None:
        """Assign the issue to a project member."""
        await self._client._call_with_retry(
            self._client.issue_api.assign_issue,
            issue_key=issue_key,
            member_id=member_id,
        )

    async def unassign_issue(self, issue_key: str) -> None:
        """Clear the issue's assignee."""
        await self._client._call_with_retry(
            self._client.issue_api.unassign_issue,
            issue_key=issue_key,
        )

    async def add_reviewer(self, issue_key: str, member_id: int) -> None:
        """Add a project member to the issue's reviewers (max 10, the assignee
        can't be a reviewer — both enforced server-side)."""
        await self._client._call_with_retry(
            self._client.issue_api.add_issue_reviewer,
            issue_key=issue_key,
            target_member_id=member_id,
        )

    async def remove_reviewer(self, issue_key: str, member_id: int) -> None:
        """Remove a reviewer from the issue."""
        await self._client._call_with_retry(
            self._client.issue_api.remove_issue_reviewer,
            issue_key=issue_key,
            target_member_id=member_id,
        )

    async def request_review(self, issue_key: str, member_ids: list[int]) -> None:
        """Ask the given (already-added) reviewers to review: resets their status
        to pending and notifies them. Does not change the reviewer roster."""
        await self._client._call_with_retry(
            self._client.issue_api.request_issue_review,
            issue_key=issue_key,
            request_review_request=RequestReviewRequest(reviewerMemberIds=member_ids),
        )

    async def submit_review(self, issue_key: str, *, approved: bool) -> None:
        """Submit the current user's review decision: `approved=True` sets their
        status to APPROVED, `approved=False` to CHANGES_REQUESTED. The caller must
        be a reviewer of the issue (server returns REVIEWER_NOT_FOUND otherwise).
        There is no way to submit PENDING — that's only the reset/initial state."""
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
        """Partial update of an issue's common fields (only provided ones are sent).

        Works around a generated-client defect: the backend wraps these fields in
        `JsonNullable<T>`, which the OpenAPI generator mis-modelled as
        `{present: bool}` with no value slot, so the typed `UpdateCommonFieldsRequest`
        can't carry values. We build the raw JSON body the server actually expects
        (`{"priority": "P1", "dueAt": null}`) and feed it through the generated
        serializer, bypassing the broken model. `clear_due_at` sends an explicit
        null to clear the due date.
        """
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
        if not body:
            return
        await self._client._call_with_retry(self._patch_common_fields, issue_key, body)

    async def _patch_common_fields(
        self, issue_key: str, body: dict[str, object | None]
    ) -> None:
        """Low-level PATCH that sends a raw dict body (see `update_common_fields`).

        Replicates the generated method's serialize/call/deserialize flow but with a
        dict in place of the unusable `UpdateCommonFieldsRequest` model.
        """
        api = self._client.issue_api
        param = api._update_issue_common_fields_serialize(
            issue_key=issue_key,
            update_common_fields_request=body,  # pyright: ignore[reportArgumentType]
            _request_auth=None,
            _content_type=None,
            _headers=None,
            _host_index=0,
        )
        response = await api.api_client.call_api(*param)
        await response.read()
        api.api_client.response_deserialize(
            response_data=response,
            response_types_map={"204": None, "400": None, "404": None},
        )

    async def update_story_point(self, issue_key: str, story_point: int | None) -> None:
        """Set (or clear, with `None`) an issue's story point."""
        await self._client._call_with_retry(
            self._client.issue_api.update_issue_story_point,
            issue_key=issue_key,
            update_story_point_request=UpdateStoryPointRequest(storyPoint=story_point),
        )
