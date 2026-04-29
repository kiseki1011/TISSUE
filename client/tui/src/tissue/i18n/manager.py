import json
import locale
from collections.abc import Callable
from pathlib import Path


class I18nManager:
    _instance = None
    _messages: dict[str, str] = {}
    _current_lang = "en"
    _listeners: list[Callable[[], None]] = []
    _lang_options_cache: list[tuple[str, str]] | None = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._load_language()
        return cls._instance

    def set_language(self, lang_code: str):
        self._current_lang = lang_code
        self._load_file(lang_code)
        for listener in list(self._listeners):
            listener()

    def subscribe(self, listener: Callable[[], None]) -> None:
        self._listeners.append(listener)

    def unsubscribe(self, listener: Callable[[], None]) -> None:
        if listener in self._listeners:
            self._listeners.remove(listener)

    def language_options(self) -> list[tuple[str, str]]:
        if self._lang_options_cache is None:
            self._lang_options_cache = self._discover_language_options()
        return self._lang_options_cache

    def _discover_language_options(self) -> list[tuple[str, str]]:
        result: list[tuple[str, str]] = []
        for path in sorted(Path(__file__).parent.glob("*.json")):
            try:
                with open(path, encoding="utf-8") as f:
                    data = json.load(f)
                display_name = data.get("__display_name__", path.stem)
            except Exception:
                display_name = path.stem
            result.append((path.stem, display_name))
        return result

    def _load_language(self):
        sys_lang, _ = locale.getdefaultlocale()
        lang_code = "en"
        if sys_lang:
            for code, _ in self.language_options():
                if sys_lang.startswith(code):
                    lang_code = code
                    break
        self._current_lang = lang_code
        self._load_file(lang_code)

    def _load_file(self, lang_code: str):
        path = Path(__file__).parent / f"{lang_code}.json"
        if not path.exists():
            path = Path(__file__).parent / "en.json"
        try:
            with open(path, encoding="utf-8") as f:
                self._messages = json.load(f)
        except Exception:
            self._messages = {}

    def get(self, key: str, **kwargs) -> str:
        msg = self._messages.get(key, key)
        if kwargs:
            try:
                return msg.format(**kwargs)
            except KeyError:
                return msg
        return msg


i18n = I18nManager()
