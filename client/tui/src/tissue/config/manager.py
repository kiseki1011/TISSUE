import logging
from datetime import datetime
from pathlib import Path

from tissue.auth.token_store import TokenStore, create_token_store
from tissue.models.config import AppConfig, BookmarkItem, ServerHistoryItem

log = logging.getLogger(__name__)

CONFIG_DIR = Path.home() / ".tissue"
CONFIG_PATH = CONFIG_DIR / "config.json"
CREDENTIALS_PATH = CONFIG_DIR / "credentials.json"


class ConfigManager:
    def __init__(self, token_store: TokenStore | None = None):
        CONFIG_DIR.mkdir(mode=0o700, exist_ok=True)
        self._token_store = token_store or create_token_store(CREDENTIALS_PATH)
        self._config: AppConfig = self._load_config()

    def _load_config(self) -> AppConfig:
        if not CONFIG_PATH.exists():
            return AppConfig()
        try:
            with open(CONFIG_PATH, encoding="utf-8") as f:
                return AppConfig.model_validate_json(f.read())
        except Exception:
            return AppConfig()

    def get_config(self) -> AppConfig:
        return self._config

    def save_server(self, url: str, server_name: str | None = None) -> None:
        previous = self._config.current_server
        if previous and previous != url:
            try:
                self._token_store.clear()
                log.info("cleared tokens on server switch (%s -> %s)", previous, url)
            except Exception as e:
                log.warning("token clear on server switch failed: %s", e)

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

    def save_tokens(self, access_token: str, refresh_token: str) -> None:
        self._token_store.save(access_token, refresh_token)

    def clear_tokens(self) -> None:
        self._token_store.clear()

    def get_tokens(self) -> tuple[str, str] | None:
        return self._token_store.load()

    def _save_to_file(self) -> None:
        with open(CONFIG_PATH, "w", encoding="utf-8") as f:
            f.write(self._config.model_dump_json(indent=2))
