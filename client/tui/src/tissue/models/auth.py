from pydantic import BaseModel, Field


class SystemSetup(BaseModel):
    mode: str = "PUBLIC"
    allow_signup: bool = Field(default=True, alias="allowSignup")
    auth_providers: list[str] = Field(default=["EMAIL"], alias="authProviders")
    model_config = {"populate_by_name": True}


class SystemInfo(BaseModel):
    status: str
    server_name: str = Field(default="Unknown Server", alias="serverName")
    setup: SystemSetup = SystemSetup()
    model_config = {"populate_by_name": True}

    def is_private(self) -> bool:
        return self.setup.mode == "PRIVATE"


class LoginRequest(BaseModel):
    login_email: str = Field(alias="loginEmail")
    password: str
    model_config = {"populate_by_name": True}


class LoginResponse(BaseModel):
    access_token: str = Field(alias="accessToken")
    refresh_token: str = Field(alias="refreshToken")
    model_config = {"populate_by_name": True}


class SignupRequest(BaseModel):
    email: str
    username: str
    password: str
    name: str
    signup_token: str = Field(alias="signupToken")
    model_config = {"populate_by_name": True}
