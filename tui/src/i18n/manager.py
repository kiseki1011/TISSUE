import json
import locale
from pathlib import Path
from typing import Dict

class I18nManager:
    _instance = None
    _messages: Dict[str, str] = {}
    _current_lang = "en"

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(I18nManager, cls).__new__(cls)
            cls._instance._load_language()
        return cls._instance

    def set_language(self, lang_code: str):
        self._current_lang = lang_code
        self._load_file(lang_code)

    def _load_language(self):
        sys_lang, _ = locale.getdefaultlocale()
        lang_code = "en"
        if sys_lang and sys_lang.startswith("ko"):
            lang_code = "ko"
        self._current_lang = lang_code
        self._load_file(lang_code)

    def _load_file(self, lang_code: str):
        path = Path(__file__).parent / f"{lang_code}.json"
        if not path.exists():
            path = Path(__file__).parent / "en.json"
        try:
            with open(path, "r", encoding="utf-8") as f:
                self._messages = json.load(f)
        except Exception:
            self._messages = {}

    def get(self, key: str, **kwargs) -> str:
        msg = self._messages.get(key, key)
        if kwargs:
            try: return msg.format(**kwargs)
            except KeyError: return msg
        return msg

i18n = I18nManager()