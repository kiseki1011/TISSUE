import httpx
from pydantic import BaseModel, Field
from typing import List, Optional

class SystemSetup(BaseModel):
    mode: str = "PUBLIC"
    allow_signup: bool = Field(default=True, alias="allowSignup")
    auth_providers: List[str] = Field(default=["EMAIL"], alias="authProviders")
    model_config = {"populate_by_name": True}

class SystemInfo(BaseModel):
    status: str
    server_name: str = Field(default="Unknown Server", alias="serverName")
    setup: SystemSetup = SystemSetup()
    model_config = {"populate_by_name": True}
    def is_private(self) -> bool:
        return self.setup.mode == "PRIVATE"

class LoginRequest(BaseModel):
    loginEmail: str
    password: str

class LoginResponse(BaseModel):
    accessToken: str
    refreshToken: str

class SignupRequest(BaseModel):
    email: str
    username: str
    password: str
    name: str

class ServerClient:
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.timeout = 5.0
        
    async def get_system_info(self) -> Optional[SystemInfo]:
        url = f"{self.base_url}/api/v1/system-info"
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(url)
                if response.status_code == 200:
                    return SystemInfo.model_validate(response.json())
                return None
        except Exception: return None

    async def login(self, email: str, password: str) -> Optional[LoginResponse]:
        url = f"{self.base_url}/api/v1/auth/login"
        payload = LoginRequest(loginEmail=email, password=password).model_dump()
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(url, json=payload)
                if response.status_code == 200:
                    return LoginResponse.model_validate(response.json())
                return None
        except Exception: return None

    async def signup(self, email: str, username: str, name: str, password: str) -> bool:
        url = f"{self.base_url}/api/v1/members/signup/email"
        payload = SignupRequest(email=email, username=username, name=name, password=password).model_dump()
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(url, json=payload)
                return response.status_code == 201
        except Exception: return False

    async def request_verification(self, email: str) -> bool:
        url = f"{self.base_url}/api/v1/members/verification/request"
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(url, json={"email": email})
                return response.status_code == 204
        except Exception: return False

    async def check_verification_status(self, email: str) -> bool:
        url = f"{self.base_url}/api/v1/members/verification/verify-status"
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(url, params={"email": email})
                if response.status_code == 200: return response.json()
                return False
        except Exception: return False