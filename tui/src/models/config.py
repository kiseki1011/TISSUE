from pydantic import BaseModel, Field
from typing import List, Optional, Union
from datetime import datetime

class ServerHistoryItem(BaseModel):
    url: str
    last_connected: datetime = Field(default_factory=datetime.now)

class AppConfig(BaseModel):
    language: str = "en"  # "en" or "ko"
    current_server: Optional[str] = None
    server_history: List[ServerHistoryItem] = []
    access_token: Optional[str] = None
    refresh_token: Optional[str] = None