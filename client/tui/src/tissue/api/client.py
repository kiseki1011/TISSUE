import logging

import httpx

from tissue.api.errors import NotTissueServer, translate
from tissue.api.generated.api.system_info_api import SystemInfoApi
from tissue.api.generated.api_client import ApiClient
from tissue.api.generated.configuration import Configuration
from tissue.api.generated.exceptions import ApiException
from tissue.api.generated.models.system_info_details import SystemInfoDetails

log = logging.getLogger(__name__)


class TissueClient:
    def __init__(self, host: str) -> None:
        normalized = host.rstrip("/")
        self._config = Configuration(host=normalized)
        self._api_client = ApiClient(configuration=self._config)
        self._system_info_api: SystemInfoApi | None = None

    @property
    def host(self) -> str:
        return self._config.host

    @property
    def system_info(self) -> SystemInfoApi:
        if self._system_info_api is None:
            self._system_info_api = SystemInfoApi(self._api_client)
        return self._system_info_api

    def set_token(self, access_token: str) -> None:
        self._config.access_token = access_token

    def clear_token(self) -> None:
        self._config.access_token = None

    async def ping(self) -> SystemInfoDetails:
        try:
            info = await self.system_info.get_system_info()
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

        if info.version is None and info.server_name is None:
            raise NotTissueServer("Response doesn't look like a Tissue server")

        return info

    async def close(self) -> None:
        await self._api_client.close()
