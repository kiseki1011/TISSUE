import httpx

from tissue.config.manager import ConfigManager


class TissueClient:
    """Base HTTP client with authentication token management"""

    def __init__(self, base_url: str, config_manager: ConfigManager | None = None):
        self.base_url = base_url.rstrip("/")
        self.config_manager = config_manager
        self.timeout = 10.0

    async def request(
        self,
        method: str,
        path: str,
        *,
        authenticated: bool = True,
        **kwargs,
    ) -> httpx.Response:
        headers = kwargs.pop("headers", {})
        if authenticated and self.config_manager:
            token = self.config_manager.get_config().access_token
            if token:
                headers["Authorization"] = f"Bearer {token}"

        async with httpx.AsyncClient(timeout=self.timeout) as http:
            response = await http.request(
                method,
                f"{self.base_url}{path}",
                headers=headers,
                **kwargs,
            )

        if response.status_code == 401 and authenticated and self.config_manager:
            refreshed = await self._try_refresh()
            if refreshed:
                token = self.config_manager.get_config().access_token
                headers["Authorization"] = f"Bearer {token}"
                async with httpx.AsyncClient(timeout=self.timeout) as http:
                    response = await http.request(
                        method,
                        f"{self.base_url}{path}",
                        headers=headers,
                        **kwargs,
                    )

        return response

    async def _try_refresh(self) -> bool:
        if not self.config_manager:
            return False
        refresh_token = self.config_manager.get_config().refresh_token
        if not refresh_token:
            return False
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as http:
                resp = await http.post(
                    f"{self.base_url}/api/v1/auth/token:refresh",
                    json={"refreshToken": refresh_token},
                )
            if resp.status_code == 200:
                data = resp.json()
                self.config_manager.save_tokens(
                    data["accessToken"], data["refreshToken"]
                )
                return True
        except Exception:
            pass
        return False
