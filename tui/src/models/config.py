from pydantic import BaseModel
from typing import List, Optional

class AppConfig(BaseModel):
    language: str = "en"  # "en" or "ko"
    current_server: Optional[str] = None
    server_history: List[str] = []
    access_token: Optional[str] = None
    refresh_token: Optional[str] = None