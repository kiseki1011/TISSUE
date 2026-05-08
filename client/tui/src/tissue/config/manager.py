import logging

from pydantic import BaseModel, Field, ValidationError

from tissue.paths import config_dir

log = logging.getLogger(__name__)


class AppSettings(BaseModel):
    """User preferences"""

    language: str = "en"
    theme: str = "textual-dark"
    vim_mode: bool = False
    border_style: str = "solid"


class AppState(BaseModel):
    """App runtime state"""

    current_server_url: str | None = None
    # TODO: current_workspace_key, current_project_key


class AppData(BaseModel):
    """Save both settings + state to a single config file"""

    settings: AppSettings = Field(default_factory=AppSettings)
    state: AppState = Field(default_factory=AppState)


class ConfigManager:
    """Settings + state saved as JSON in the OS config directory"""

    def __init__(self) -> None:
        self._path = config_dir() / "config.json"
        self._data: AppData = self._load()

    @property
    def settings(self) -> AppSettings:
        return self._data.settings

    @property
    def state(self) -> AppState:
        return self._data.state

    def update_settings(self, **kwargs: object) -> None:
        """Update user settings and save"""
        self._data.settings = self._data.settings.model_copy(update=kwargs)
        self._save()

    def update_state(self, **kwargs: object) -> None:
        """Update app state and save"""
        self._data.state = self._data.state.model_copy(update=kwargs)
        self._save()

    def _load(self) -> AppData:
        if not self._path.exists():
            log.debug("no config at %s, using defaults", self._path)
            return AppData()
        try:
            return AppData.model_validate_json(self._path.read_text(encoding="utf-8"))
        except (OSError, ValidationError, ValueError) as e:
            log.warning("failed to load %s: %s, using defaults", self._path, e)
            return AppData()

    def _save(self) -> None:
        self._path.parent.mkdir(parents=True, exist_ok=True)
        try:
            self._path.write_text(
                self._data.model_dump_json(indent=2),
                encoding="utf-8",
            )
        except OSError as e:
            log.error("failed to save %s: %s", self._path, e)
