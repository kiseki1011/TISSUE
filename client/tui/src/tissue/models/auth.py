from pydantic import BaseModel, Field


class TokenPair(BaseModel):
    access_token: str = Field(alias="accessToken")
    refresh_token: str = Field(alias="refreshToken")
    model_config = {"populate_by_name": True}
