import os
import sys
from pathlib import Path

_APP_NAME = "tissue"


def state_dir() -> Path:
    """Return the log/state directory.
    $XDG_STATE_HOME on default, %LOCALAPPDATA% on Windows.
    """
    if sys.platform == "win32":
        base = os.environ.get("LOCALAPPDATA") or str(Path.home() / "AppData" / "Local")
        return Path(base) / _APP_NAME

    base = os.environ.get("XDG_STATE_HOME") or str(Path.home() / ".local" / "state")
    return Path(base) / _APP_NAME


def config_dir() -> Path:
    """Return the user config directory.
    $XDG_CONFIG_HOME on default, %APPDATA% on Windows.
    """
    if sys.platform == "win32":
        base = os.environ.get("APPDATA") or str(Path.home() / "AppData" / "Roaming")
        return Path(base) / _APP_NAME

    base = os.environ.get("XDG_CONFIG_HOME") or str(Path.home() / ".config")
    return Path(base) / _APP_NAME


def credentials_path() -> Path:
    """Token store fallback (when keyring unavailable)"""
    return state_dir() / "credentials.json"
