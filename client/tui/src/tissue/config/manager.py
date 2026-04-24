import json
from datetime import datetime
from pathlib import Path

from tissue.models.config import AppConfig, BookmarkItem, ServerHistoryItem

CONFIG_PATH = Path("config.json")


class ConfigManager:
    def __init__(self):
        self._config: AppConfig = self._load_config()

    def _load_config(self) -> AppConfig:
        if not CONFIG_PATH.exists():
            return AppConfig()

        try:
            with open(CONFIG_PATH, encoding="utf-8") as f:
                data = json.load(f)
                if "server_history" in data and data["server_history"]:
                    if isinstance(data["server_history"][0], str):
                        data["server_history"] = [
                            {"url": url, "last_connected": datetime.now().isoformat()}
                            for url in data["server_history"]
                        ]
                if "bookmarks" in data:
                    for b in data["bookmarks"]:
                        if (
                            isinstance(b, dict)
                            and "alias" in b
                            and "description" not in b
                        ):
                            b["description"] = b.pop("alias")
                return AppConfig(**data)
        except Exception:
            return AppConfig()

    def get_config(self) -> AppConfig:
        return self._config

    def save_server(self, url: str, server_name: str | None = None) -> None:
        self._config.current_server = url

        existing = next(
            (item for item in self._config.server_history if item.url == url), None
        )
        if existing:
            existing.last_connected = datetime.now()
            if server_name is not None:
                existing.server_name = server_name
            self._config.server_history.remove(existing)
            self._config.server_history.insert(0, existing)
        else:
            self._config.server_history.insert(
                0, ServerHistoryItem(url=url, server_name=server_name)
            )

        for b in self._config.bookmarks:
            if b.url == url and server_name is not None:
                b.server_name = server_name

        self._save_to_file()

    def remove_history_item(self, url: str) -> None:
        self._config.server_history = [
            h for h in self._config.server_history if h.url != url
        ]
        self._save_to_file()

    def add_bookmark(
        self,
        url: str,
        server_name: str | None = None,
        description: str | None = None,
    ) -> None:
        existing = next(
            (item for item in self._config.bookmarks if item.url == url), None
        )
        if existing:
            if server_name is not None:
                existing.server_name = server_name
            if description is not None:
                existing.description = description
        else:
            self._config.bookmarks.append(
                BookmarkItem(url=url, server_name=server_name, description=description)
            )
        self._save_to_file()

    def remove_bookmark(self, url: str) -> None:
        self._config.bookmarks = [b for b in self._config.bookmarks if b.url != url]
        self._save_to_file()

    def is_bookmarked(self, url: str) -> bool:
        return any(b.url == url for b in self._config.bookmarks)

    def save_tokens(self, access_token: str, refresh_token: str):
        self._config.access_token = access_token
        self._config.refresh_token = refresh_token
        self._save_to_file()

    def clear_tokens(self):
        self._config.access_token = None
        self._config.refresh_token = None
        self._save_to_file()

    def _save_to_file(self):
        with open(CONFIG_PATH, "w", encoding="utf-8") as f:
            f.write(self._config.model_dump_json(indent=2))
