import json
from pathlib import Path
from datetime import datetime
from src.models.config import AppConfig, ServerHistoryItem

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
                # Migration: Convert old string list to objects if needed
                if "server_history" in data and data["server_history"]:
                    if isinstance(data["server_history"][0], str):
                        data["server_history"] = [
                            {"url": url, "last_connected": datetime.now().isoformat()} 
                            for url in data["server_history"]
                        ]
                return AppConfig(**data)
        except Exception:
            return AppConfig()

    def get_config(self) -> AppConfig:
        return self._config

    def save_server(self, url: str):
        self._config.current_server = url
        
        # Update or add history item
        existing = next((item for item in self._config.server_history if item.url == url), None)
        if existing:
            existing.last_connected = datetime.now()
            # Move to top
            self._config.server_history.remove(existing)
            self._config.server_history.insert(0, existing)
        else:
            self._config.server_history.insert(0, ServerHistoryItem(url=url))
            
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