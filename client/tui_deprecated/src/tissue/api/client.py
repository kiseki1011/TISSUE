import asyncio
import logging

import httpx

from tissue.api.errors import ApiInvalidUrlError, ApiNetworkError
from tissue.config.manager import ConfigManager
from tissue.models.auth import TokenPair

log = logging.getLogger(__name__)


class TissueClient:
    def __init__(self, config_manager: ConfigManager | None = None):
        self.config_manager = config_manager
        self._http = httpx.AsyncClient(
            timeout=httpx.Timeout(connect=3.0, read=10.0, write=10.0, pool=5.0),
        )
        self._refresh_lock = asyncio.Lock()

    def set_base_url(self, base_url: str) -> None:
        self._http.base_url = base_url.rstrip("/")

    async def aclose(self) -> None:
        await self._http.aclose()

    async def request(
        self,
        method: str,
        path: str,
        *,
        authenticated: bool = True,
        **kwargs,
    ) -> httpx.Response:
        headers = self._auth_headers(kwargs.pop("headers", {}), authenticated)
        expired_token = (
            self._access_token() if authenticated and self.config_manager else None
        )

        response = await self._send(method, path, headers, **kwargs)

        if (
            response.status_code == 401
            and authenticated
            and expired_token
            and self.config_manager
        ):
            if await self._try_refresh(expired_token):
                headers = self._auth_headers(headers, authenticated=True)
                response = await self._send(method, path, headers, **kwargs)

        return response

    async def _send(
        self, method: str, path: str, headers: dict, **kwargs
    ) -> httpx.Response:
        try:
            return await self._http.request(method, path, headers=headers, **kwargs)
        except (httpx.UnsupportedProtocol, httpx.InvalidURL) as e:
            raise ApiInvalidUrlError(str(e)) from e
        except httpx.RequestError as e:
            raise ApiNetworkError(str(e)) from e

    def _auth_headers(self, headers: dict, authenticated: bool) -> dict:
        if not authenticated or not self.config_manager:
            return headers
        token = self._access_token()
        if token:
            headers["Authorization"] = f"Bearer {token}"
        return headers

    def _access_token(self) -> str | None:
        tokens = self.config_manager.get_tokens() if self.config_manager else None
        return tokens.access_token if tokens else None

    def _refresh_token(self) -> str | None:
        tokens = self.config_manager.get_tokens() if self.config_manager else None
        return tokens.refresh_token if tokens else None

    async def _try_refresh(self, expired_access: str) -> bool:
        if not self.config_manager:
            return False
        async with self._refresh_lock:
            current = self._access_token()
            if current != expired_access:
                return True

            refresh_token = self._refresh_token()
            if not refresh_token:
                return False

            try:
                resp = await self._http.post(
                    "/api/v1/auth/token:refresh",
                    json={"refreshToken": refresh_token},
                )
            except (httpx.RequestError, httpx.InvalidURL) as e:
                log.warning("Token refresh request failed: %s", e)
                return False

            if resp.status_code == 200:
                self.config_manager.save_tokens(TokenPair.model_validate(resp.json()))
                return True
            log.warning("Token refresh failed: HTTP %d", resp.status_code)
            return False
