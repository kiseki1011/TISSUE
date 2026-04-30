import logging

import pydantic

from tissue.api.client import TissueClient
from tissue.api.errors import ApiResponseError, ApiSchemaError
from tissue.models.workspace import (
    CreateWorkspaceRequest,
    WorkspaceCreateResponse,
    WorkspaceSummary,
)

log = logging.getLogger(__name__)


class WorkspaceAPI:
    def __init__(self, client: TissueClient):
        self.client = client

    async def list_my(self) -> list[WorkspaceSummary]:
        resp = await self.client.request("GET", "/api/v1/workspaces/me")
        if resp.status_code != 200:
            raise ApiResponseError.from_response(resp)
        try:
            return [WorkspaceSummary.model_validate(item) for item in resp.json()]
        except pydantic.ValidationError as e:
            raise ApiSchemaError(str(e)) from e

    async def create(self, req: CreateWorkspaceRequest) -> WorkspaceCreateResponse:
        payload = req.model_dump(by_alias=True, exclude_none=True)
        resp = await self.client.request("POST", "/api/v1/workspaces", json=payload)
        if resp.status_code != 201:
            raise ApiResponseError.from_response(resp)
        try:
            return WorkspaceCreateResponse.model_validate(resp.json())
        except pydantic.ValidationError as e:
            raise ApiSchemaError(str(e)) from e
