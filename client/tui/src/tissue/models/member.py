from datetime import datetime

from pydantic import BaseModel, Field


class MemberProfile(BaseModel):
    email: str | None = None
    username: str
    name: str
    joined_at: datetime = Field(alias="joinedAt")
    last_updated_at: datetime | None = Field(default=None, alias="lastUpdatedAt")
    model_config = {"populate_by_name": True}
