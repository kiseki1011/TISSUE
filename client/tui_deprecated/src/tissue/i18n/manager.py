import json
import logging
import os
from collections.abc import Callable
from pathlib import Path

log = logging.getLogger(__name__)

DISPLAY_NAME_KEY = "__display_name__"


class I18nManager:
    """Internationalization manager with observer pattern.
    Import `i18n` from this module. Do not instantiate this class directly.
    """

    def __init__(self) -> None:
        self._messages: dict[str, str] = {}
        self._current_lang: str = "en"
        self._listeners: list[Callable[[], None]] = []
        self._lang_options_cache: list[tuple[str, str]] | None = None
        self._load_language()

    def set_language(self, lang_code: str) -> None:
        self._current_lang = lang_code
        self._load_file(lang_code)
        for listener in list(self._listeners):
            listener()

    def subscribe(self, listener: Callable[[], None]) -> None:
        if listener not in self._listeners:
            self._listeners.append(listener)

    def unsubscribe(self, listener: Callable[[], None]) -> None:
        if listener in self._listeners:
            self._listeners.remove(listener)

    def language_options(self) -> list[tuple[str, str]]:
        if self._lang_options_cache is None:
            self._lang_options_cache = self._discover_language_options()
        return self._lang_options_cache

    def _discover_language_options(self) -> list[tuple[str, str]]:
        """Scan *.json files in this directory to build the language list.
        (Do not move the translation files from this directory)

        For each file:
          - code: file name without extension (`en.json` -> `en`)
          - display_name: JSON's `__display_name__` value, uses `code` if missing

        Returns:
            list[tuple[str, str]]: List of (code, display_name) tuples.
        """
        result: list[tuple[str, str]] = []
        for path in sorted(Path(__file__).parent.glob("*.json")):
            display_name = path.stem
            try:
                with open(path, encoding="utf-8") as f:
                    data = json.load(f)
                display_name = data.get(DISPLAY_NAME_KEY, path.stem)
            except (OSError, json.JSONDecodeError) as e:
                log.warning("Failed to read language file %s: %s", path, e)
            result.append((path.stem, display_name))
        return result

    def _load_language(self) -> None:
        sys_lang = self._detect_system_language()
        lang_code = "en"
        if sys_lang:
            for code, _ in self.language_options():
                if sys_lang.startswith(code):
                    lang_code = code
                    break
        self._current_lang = lang_code
        self._load_file(lang_code)

    @staticmethod
    def _detect_system_language() -> str:
        for env_var in ("LC_ALL", "LC_MESSAGES", "LANG"):
            value = os.environ.get(env_var)
            if value and value not in ("C", "POSIX"):
                return value
        return ""

    def _load_file(self, lang_code: str) -> None:
        path = Path(__file__).parent / f"{lang_code}.json"
        if not path.exists():
            log.warning("Language file %s not found, falling back to en.json", path)
            path = Path(__file__).parent / "en.json"
        try:
            with open(path, encoding="utf-8") as f:
                self._messages = json.load(f)
        except (OSError, json.JSONDecodeError) as e:
            log.error("Failed to load language file %s: %s", path, e)
            self._messages = {}

    def get(self, key: str, **kwargs) -> str:
        msg = self._messages.get(key, key)
        if kwargs:
            try:
                return msg.format(**kwargs)
            except (KeyError, IndexError) as e:
                log.debug("Format failed for key %r with args %r: %s", key, kwargs, e)
                return msg
        return msg


i18n = I18nManager()
