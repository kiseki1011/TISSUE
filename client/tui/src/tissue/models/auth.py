from pydantic import BaseModel, ConfigDict, Field


class TokenPair(BaseModel):
    access_token: str
    refresh_token: str


class OidcDeviceStart(BaseModel):
    """Response of POST /auth/oidc/device:start (RFC 8628 device authorization)."""

    model_config = ConfigDict(populate_by_name=True)

    user_code: str = Field(alias="userCode")
    verification_uri: str = Field(alias="verificationUri")
    verification_uri_complete: str | None = Field(
        default=None, alias="verificationUriComplete"
    )
    device_code: str = Field(alias="deviceCode")
    interval: int = 5
    expires_in: int = Field(default=600, alias="expiresIn")


class OidcDevicePoll(BaseModel):
    """Response of POST /auth/oidc/device:poll."""

    model_config = ConfigDict(populate_by_name=True)

    status: str
    access_token: str | None = Field(default=None, alias="accessToken")
    refresh_token: str | None = Field(default=None, alias="refreshToken")
