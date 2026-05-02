import logging

import pydantic

from tissue.api.client import TissueClient
from tissue.api.errors import ApiResponseError, ApiSchemaError
from tissue.models.invitation import InvitationSummary

log = logging.getLogger(__name__)


class InvitationAPI:
    def __init__(self, client: TissueClient):
        self.client = client

    async def list_my(self) -> list[InvitationSummary]:
        resp = await self.client.request("GET", "/api/v1/invitations")
        if resp.status_code != 200:
            raise ApiResponseError.from_response(resp)
        try:
            return [InvitationSummary.model_validate(item) for item in resp.json()]
        except pydantic.ValidationError as e:
            raise ApiSchemaError(str(e)) from e

    async def accept(self, invitation_id: int) -> None:
        resp = await self.client.request(
            "POST", f"/api/v1/invitations/{invitation_id}:accept"
        )
        if resp.status_code != 204:
            raise ApiResponseError.from_response(resp)

    async def reject(self, invitation_id: int) -> None:
        resp = await self.client.request(
            "POST", f"/api/v1/invitations/{invitation_id}:reject"
        )
        if resp.status_code != 204:
            raise ApiResponseError.from_response(resp)
