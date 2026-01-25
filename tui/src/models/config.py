from pydantic import BaseModel
from typing import List, Optional

class AppConfig(BaseModel):
    current_server: Optional[str] = None
    server_history: List[str] = []
