import json
from pathlib import Path
from src.models.config import AppConfig

CONFIG_PATH = Path("config.json")

class ConfigManager:
    def __init__(self):
        self._config: AppConfig = self._load_config()

    def _load_config(self) -> AppConfig:
        if not CONFIG_PATH.exists():
            return AppConfig()
        
        try:
            with open(CONFIG_PATH, "r", encoding="utf-8") as f:
                data = json.load(f)
                return AppConfig(**data)
        except Exception:
            return AppConfig()

    def get_config(self) -> AppConfig:
        return self._config

    def save_server(self, url: str):
        self._config.current_server = url
        if url not in self._config.server_history:
            self._config.server_history.append(url)
        self._save_to_file()

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