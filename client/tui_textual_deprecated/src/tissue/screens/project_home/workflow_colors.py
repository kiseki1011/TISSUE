from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from tissue.api.errors import TissueApiError
from tissue.widgets.color_type import color_hex

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.workflow_detail import WorkflowDetail


async def load_state_colors(
    client: TissueClient,
    workflow_cache: dict[int, WorkflowDetail],
    log: logging.Logger,
) -> dict[int, str]:
    try:
        summaries = await client.workflows.list_workflows()
    except TissueApiError as error:
        log.debug("Hub: failed to list workflows: %s", error)
        return {}

    colors: dict[int, str] = {}
    for summary in summaries:
        workflow_id = summary.id
        if workflow_id is None:
            continue
        workflow = workflow_cache.get(workflow_id)
        if workflow is None:
            try:
                workflow = await client.workflows.get_workflow(workflow_id)
            except TissueApiError as error:
                log.debug("Hub: failed to load workflow %s: %s", workflow_id, error)
                continue
            workflow_cache[workflow_id] = workflow
        for state in workflow.states or []:
            if state.id is None or not state.color:
                continue
            hex_color = color_hex(state.color)
            if hex_color:
                colors[state.id] = hex_color
    return colors
