from datetime import datetime

from pydantic import BaseModel, Field


class ServerHistoryItem(BaseModel):
    url: str
    last_connected: datetime = Field(default_factory=datetime.now)


class AppConfig(BaseModel):
    language: str = "en"
    current_server: str | None = None
    server_history: list[ServerHistoryItem] = []
    access_token: str | None = None
    refresh_token: str | None = None
