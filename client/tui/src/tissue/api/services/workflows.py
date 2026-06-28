from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.workflow_detail import WorkflowDetail
    from tissue.api.generated.models.workflow_summary import WorkflowSummary


class WorkflowService:
    """Workflow read operations."""

    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def list_workflows(self) -> list[WorkflowSummary]:
        """All workflows, summaries only with no state graph."""
        return await self._client._call_with_retry(
            self._client.workflow_api.list_workflows,
        )

    async def get_workflow(self, workflow_id: int) -> WorkflowDetail:
        """A workflow's full state graph, states and transitions with targets."""
        return await self._client._call_with_retry(
            self._client.workflow_api.get_workflow,
            workflow_id,
        )
