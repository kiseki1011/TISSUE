from datetime import datetime

from tissue.models.auth import SystemInfo, SystemSetup
from tissue.models.config import ServerHistoryItem

STUB_SYSTEM_INFO = SystemInfo(
    status="RUNNING",
    server_name="Dev Tissue Server",
    setup=SystemSetup(
        allow_signup=True,
        email_required=False,
        auth_providers=["EMAIL", "GITHUB"],
    ),
)

STUB_SERVER_HISTORY = [
    ServerHistoryItem(
        url="http://localhost:8080",
        last_connected=datetime(2026, 4, 17, 14, 30),
    ),
    ServerHistoryItem(
        url="http://tissue.example.com",
        last_connected=datetime(2026, 4, 14, 10, 0),
    ),
    ServerHistoryItem(
        url="http://192.168.1.100:8080",
        last_connected=datetime(2026, 4, 11, 9, 15),
    ),
]

STUB_REGISTERED_URLS = {item.url for item in STUB_SERVER_HISTORY}

STUB_ACCOUNTS: dict[str, dict] = {
    "stub@trytissue.dev": {
        "password": "stub1234!",
        "access_token": "stub_access",
        "refresh_token": "stub_refresh",
    },
}

STUB_TAKEN_EMAILS: set[str] = {"taken@example.com", "stub@trytissue.dev"}

STUB_TAKEN_USERNAMES: set[str] = {"admin", "root", "tissue"}
