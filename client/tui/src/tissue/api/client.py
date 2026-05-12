import logging

import httpx

from tissue.api.errors import NotTissueServer, TissueApiError, translate
from tissue.api.generated.api.authentication_api import AuthenticationApi
from tissue.api.generated.api.system_info_api import SystemInfoApi
from tissue.api.generated.api_client import ApiClient
from tissue.api.generated.configuration import Configuration
from tissue.api.generated.exceptions import ApiException
from tissue.api.generated.models.login_request import LoginRequest
from tissue.api.generated.models.system_info_details import SystemInfoDetails
from tissue.models.auth import TokenPair

log = logging.getLogger(__name__)


class TissueClient:
    def __init__(self, host: str) -> None:
        normalized = host.rstrip("/")
        self._config = Configuration(host=normalized)
        self._api_client = ApiClient(configuration=self._config)
        self._system_info_api: SystemInfoApi | None = None
        self._auth_api: AuthenticationApi | None = None

    @property
    def host(self) -> str:
        return self._config.host

    @property
    def system_info(self) -> SystemInfoApi:
        if self._system_info_api is None:
            self._system_info_api = SystemInfoApi(self._api_client)
        return self._system_info_api

    @property
    def auth(self) -> AuthenticationApi:
        if self._auth_api is None:
            self._auth_api = AuthenticationApi(self._api_client)
        return self._auth_api

    def set_token(self, access_token: str) -> None:
        self._config.access_token = access_token

    def clear_token(self) -> None:
        self._config.access_token = None

    async def ping(self) -> SystemInfoDetails:
        """Probe /system-info. Raises if unreachable or not a Tissue server."""
        try:
            info = await self.system_info.get_system_info()
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

        if info.version is None:
            raise NotTissueServer("The tissue server always comes with a version")

        return info

    async def login(self, identifier: str, password: str) -> TokenPair:
        request = LoginRequest(identifier=identifier, password=password)
        try:
            response = await self.auth.login(request)
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

        if response.access_token is None or response.refresh_token is None:
            raise TissueApiError("Server returned incomplete login response")

        token = TokenPair(
            access_token=response.access_token,
            refresh_token=response.refresh_token,
        )
        self.set_token(token.access_token)
        return token

    async def close(self) -> None:
        await self._api_client.close()
