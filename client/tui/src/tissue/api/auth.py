from tissue.api.client import TissueClient
from tissue.models.auth import (
    LoginRequest,
    LoginResponse,
    SystemInfo,
)


class AuthAPI:
    def __init__(self, client: TissueClient):
        self.client = client

    async def get_system_info(self) -> SystemInfo | None:
        try:
            resp = await self.client.request(
                "GET", "/api/v1/system-info", authenticated=False
            )
            if resp.status_code == 200:
                return SystemInfo.model_validate(resp.json())
            return None
        except Exception:
            return None

    async def login(self, email: str, password: str) -> LoginResponse | None:
        payload = LoginRequest(login_email=email, password=password).model_dump(
            by_alias=True
        )
        try:
            resp = await self.client.request(
                "POST", "/api/v1/auth/login", json=payload, authenticated=False
            )
            if resp.status_code == 200:
                return LoginResponse.model_validate(resp.json())
            return None
        except Exception:
            return None

    async def logout(self, refresh_token: str) -> bool:
        try:
            resp = await self.client.request(
                "POST",
                "/api/v1/auth/logout",
                json={"refreshToken": refresh_token},
            )
            return resp.status_code == 204
        except Exception:
            return False
