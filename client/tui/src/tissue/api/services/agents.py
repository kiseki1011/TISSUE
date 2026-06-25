from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.agent_response import AgentResponse


# VIBE-CODED
# model: "claude-opus-4-8"
# evaluation: NOT_REVIEWED
class AgentService:
    """Agent (MCP) operations scoped to the current owner."""

    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def list_my_agents(self) -> list[AgentResponse]:
        """List the agents the current user owns.

        Each agent's `.id` is its agent member id, used to find the work
        assigned to it.
        """
        return await self._client._call_with_retry(
            self._client.agents_api.list_my_agents,
        )
