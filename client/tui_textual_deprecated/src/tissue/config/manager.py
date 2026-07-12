import logging
from datetime import datetime

from pydantic import BaseModel, Field, ValidationError

from tissue.paths import config_dir

log = logging.getLogger(__name__)


class AppSettings(BaseModel):
    """User preferences."""

    theme: str = "tokyo-night"
    # Folder for offline wiki drafts. None falls back to paths.drafts_dir().
    wiki_draft_dir: str | None = None


class AppState(BaseModel):
    """App runtime state."""

    current_server_url: str | None = None
    last_connected_at: datetime | None = None

    # The project hub open when the app last closed, restored on next launch.
    # None means the user was on the dashboard.
    current_project_key: str | None = None
    current_project_by_server: dict[str, str | None] = Field(default_factory=dict)

    seen_logins: dict[str, list[str]] = Field(default_factory=dict)
    pinned_projects: dict[str, list[str]] = Field(default_factory=dict)
    project_filters: dict[str, dict] = Field(default_factory=dict)
    project_filters_by_server: dict[str, dict[str, dict]] = Field(default_factory=dict)
    project_ui_by_server: dict[str, dict[str, dict]] = Field(default_factory=dict)


class AppData(BaseModel):
    settings: AppSettings = Field(default_factory=AppSettings)
    state: AppState = Field(default_factory=AppState)


class ConfigManager:
    """Settings and state saved as JSON in the OS config directory."""

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
        self._data.settings = self._data.settings.model_copy(update=kwargs)
        self._save()

    def update_state(self, **kwargs: object) -> None:
        self._data.state = self._data.state.model_copy(update=kwargs)
        self._save()

    def set_current_server(self, server_url: str) -> None:
        previous = self._data.state.current_server_url
        if previous == server_url:
            self.update_state(current_server_url=server_url)
            return

        projects = dict(self._data.state.current_project_by_server)
        filters_by_server = self._copy_nested_project_state(
            self._data.state.project_filters_by_server
        )
        legacy_filters = self._copy_project_state(self._data.state.project_filters)

        if previous:
            projects.setdefault(previous, self._data.state.current_project_key)
            if legacy_filters:
                saved = filters_by_server.setdefault(previous, {})
                for key, value in legacy_filters.items():
                    saved.setdefault(key, value)
                legacy_filters = {}

        self.update_state(
            current_server_url=server_url,
            current_project_key=projects.get(server_url),
            current_project_by_server=projects,
            project_filters=legacy_filters,
            project_filters_by_server=filters_by_server,
        )

    def is_first_login(self, server_url: str, username: str) -> bool:
        """`True` when this (`server_url`, `username`) pair has not logged in here."""
        return username not in self._data.state.seen_logins.get(server_url, [])

    def mark_login_seen(self, server_url: str, username: str) -> None:
        seen = {k: list(v) for k, v in self._data.state.seen_logins.items()}
        users = seen.setdefault(server_url, [])
        if username in users:
            return
        users.append(username)
        self.update_state(seen_logins=seen)

    def set_last_project(self, project_key: str | None) -> None:
        """Remember the project hub now open (None for the dashboard)."""
        server = self._data.state.current_server_url
        if not server:
            self.update_state(current_project_key=project_key)
            return

        projects = dict(self._data.state.current_project_by_server)
        projects[server] = project_key
        self.update_state(
            current_project_key=project_key,
            current_project_by_server=projects,
        )

    def last_project(self, server_url: str | None = None) -> str | None:
        server = server_url or self._data.state.current_server_url
        if server and server in self._data.state.current_project_by_server:
            return self._data.state.current_project_by_server[server]
        return self._data.state.current_project_key

    def project_filter_state(self, project_key: str) -> dict:
        """The saved filter bundle for a project, empty when none was stored."""
        server = self._data.state.current_server_url
        if server:
            scoped = self._data.state.project_filters_by_server.get(server, {})
            if project_key in scoped:
                return dict(scoped[project_key])
        return dict(self._data.state.project_filters.get(project_key, {}))

    def save_project_filters(self, project_key: str, filters: dict) -> None:
        server = self._data.state.current_server_url
        if not server:
            all_filters = self._copy_project_state(self._data.state.project_filters)
            all_filters[project_key] = dict(filters)
            self.update_state(project_filters=all_filters)
            return

        filters_by_server = self._copy_nested_project_state(
            self._data.state.project_filters_by_server
        )
        filters_by_server.setdefault(server, {})[project_key] = dict(filters)
        self.update_state(project_filters_by_server=filters_by_server)

    def project_ui_state(self, project_key: str) -> dict:
        server = self._data.state.current_server_url
        if not server:
            return {}
        return dict(
            self._data.state.project_ui_by_server.get(server, {}).get(project_key, {})
        )

    def save_project_ui_state(self, project_key: str, ui_state: dict) -> None:
        server = self._data.state.current_server_url
        if not server:
            return
        ui_by_server = self._copy_nested_project_state(
            self._data.state.project_ui_by_server
        )
        ui_by_server.setdefault(server, {})[project_key] = dict(ui_state)
        self.update_state(project_ui_by_server=ui_by_server)

    def pinned_project_keys(self, server_url: str) -> list[str]:
        return list(self._data.state.pinned_projects.get(server_url, []))

    def toggle_pinned_project(self, server_url: str, project_key: str) -> bool:
        """Pin or unpin a project for a server, returning the new pinned state."""
        pinned = {k: list(v) for k, v in self._data.state.pinned_projects.items()}
        keys = pinned.setdefault(server_url, [])
        if project_key in keys:
            keys.remove(project_key)
            now_pinned = False
        else:
            keys.append(project_key)
            now_pinned = True
        self.update_state(pinned_projects=pinned)
        return now_pinned

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

    @staticmethod
    def _copy_project_state(state: dict[str, dict]) -> dict[str, dict]:
        return {project: dict(value) for project, value in state.items()}

    @classmethod
    def _copy_nested_project_state(
        cls, state: dict[str, dict[str, dict]]
    ) -> dict[str, dict[str, dict]]:
        return {
            server: cls._copy_project_state(projects)
            for server, projects in state.items()
        }
