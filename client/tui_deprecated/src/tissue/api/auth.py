import logging

import pydantic

from tissue.api.client import TissueClient
from tissue.api.errors import ApiResponseError, ApiSchemaError
from tissue.models.auth import (
    LoginRequest,
    SystemInfo,
    TokenPair,
)

log = logging.getLogger(__name__)


class AuthAPI:
    def __init__(self, client: TissueClient):
        self.client = client

    async def get_system_info(self) -> SystemInfo:
        resp = await self.client.request(
            "GET", "/api/v1/system-info", authenticated=False
        )
        if resp.status_code != 200:
            raise ApiResponseError.from_response(resp)
        try:
            return SystemInfo.model_validate(resp.json())
        except pydantic.ValidationError as e:
            raise ApiSchemaError(str(e)) from e

    async def login(self, identifier: str, password: str) -> TokenPair:
        payload = LoginRequest(identifier=identifier, password=password).model_dump(
            by_alias=True
        )
        resp = await self.client.request(
            "POST", "/api/v1/auth/login", json=payload, authenticated=False
        )
        if resp.status_code != 200:
            raise ApiResponseError.from_response(resp)
        try:
            return TokenPair.model_validate(resp.json())
        except pydantic.ValidationError as e:
            raise ApiSchemaError(str(e)) from e

    async def logout(self, refresh_token: str) -> None:
        resp = await self.client.request(
            "POST",
            "/api/v1/auth/logout",
            json={"refreshToken": refresh_token},
        )
        if resp.status_code != 204:
            raise ApiResponseError.from_response(resp)
