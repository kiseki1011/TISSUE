from datetime import datetime

from tissue.models.auth import SystemInfo, SystemSetup
from tissue.models.config import ServerHistoryItem

MOCK_SYSTEM_INFO = SystemInfo(
    status="RUNNING",
    server_name="Dev Tissue Server",
    setup=SystemSetup(
        mode="PUBLIC",
        allow_signup=True,
        auth_providers=["EMAIL", "GITHUB"],
    ),
)

MOCK_SERVER_HISTORY = [
    ServerHistoryItem(
        url="http://localhost:8080",
        last_connected=datetime(2026, 4, 19, 14, 30),
    ),
    ServerHistoryItem(
        url="http://tissue.example.com",
        last_connected=datetime(2026, 4, 18, 10, 0),
    ),
    ServerHistoryItem(
        url="http://192.168.1.100:8080",
        last_connected=datetime(2026, 4, 15, 9, 15),
    ),
]
