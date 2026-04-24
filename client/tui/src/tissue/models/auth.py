from pydantic import BaseModel, Field


class SystemSetup(BaseModel):
    allow_signup: bool = Field(default=True, alias="allowSignup")
    email_required: bool = Field(default=True, alias="emailRequired")
    auth_providers: list[str] = Field(default=["EMAIL"], alias="authProviders")
    model_config = {"populate_by_name": True}


class SystemInfo(BaseModel):
    version: str | None = None
    server_name: str = Field(default="Unknown Server", alias="serverName")
    setup: SystemSetup = SystemSetup()
    model_config = {"populate_by_name": True}

    def is_email_required(self) -> bool:
        return self.setup.email_required


class LoginRequest(BaseModel):
    identifier: str
    password: str


class LoginResponse(BaseModel):
    access_token: str = Field(alias="accessToken")
    refresh_token: str = Field(alias="refreshToken")
    model_config = {"populate_by_name": True}


class SignupRequest(BaseModel):
    email: str
    username: str
    password: str
    name: str
    verified_token: str = Field(alias="verifiedToken")
    model_config = {"populate_by_name": True}
