import json
import logging
from pathlib import Path

log = logging.getLogger(__name__)


class I18n:
    """i18n manager. Refresh is handled by Screen."""

    def __init__(self) -> None:
        self._messages: dict[str, str] = {}
        self._current_lang: str = "en"
        self._load()

    @property
    def current_lang(self) -> str:
        return self._current_lang

    def set_language(self, lang: str) -> None:
        self._current_lang = lang
        self._load()

    def get(self, key: str, /, **kwargs: object) -> str:
        # `key` is positional-only so a format placeholder named `{key}` can be
        # passed as a kwarg (e.g. get("...{key}...", key=value)) without clashing.
        msg = self._messages.get(key, key)
        return msg.format(**kwargs) if kwargs else msg

    def language_options(self) -> list[tuple[str, str]]:
        """Return the (code, display_name) list by scanning *.json files

        Returns:
            list[tuple[str, str]]: [(code, display_name), ...]
        """
        result: list[tuple[str, str]] = []
        for path in sorted(Path(__file__).parent.glob("*.json")):
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
                display = data.get("__display_name__", path.stem)
            except (OSError, json.JSONDecodeError) as e:
                log.warning("Failed to read %s: %s", path, e)
                display = path.stem
            result.append((path.stem, display))
        return result

    def _load(self) -> None:
        path = Path(__file__).parent / f"{self._current_lang}.json"
        if not path.exists():
            log.warning("Language file %s not found, falling back to 'en'", path)
            path = Path(__file__).parent / "en.json"
        try:
            self._messages = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as e:
            log.error("Failed to load %s: %s", path, e)
            self._messages = {}


i18n = I18n()
