from datetime import datetime

from pydantic import BaseModel, Field


class ServerHistoryItem(BaseModel):
    url: str
    server_name: str | None = None
    last_connected: datetime = Field(default_factory=datetime.now)


class BookmarkItem(BaseModel):
    url: str
    server_name: str | None = None
    description: str | None = None
    created_at: datetime = Field(default_factory=datetime.now)


class AppConfig(BaseModel):
    language: str = "en"
    current_server: str | None = None
    server_history: list[ServerHistoryItem] = []
    bookmarks: list[BookmarkItem] = []
