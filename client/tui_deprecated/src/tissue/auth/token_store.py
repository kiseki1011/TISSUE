import json
import logging
import os
from pathlib import Path
from typing import Protocol

import keyring
import keyring.errors
import pydantic

from tissue.models.auth import TokenPair

log = logging.getLogger(__name__)

KEYRING_SERVICE = "tissue-tui"
KEYRING_USERNAME = "default"


class TokenStore(Protocol):
    def load(self) -> TokenPair | None: ...

    def save(self, token_pair: TokenPair) -> None: ...

    def clear(self) -> None: ...


class KeyringTokenStore:
    def __init__(
        self,
        service: str = KEYRING_SERVICE,
        username: str = KEYRING_USERNAME,
    ):
        self._service = service
        self._username = username

    def load(self) -> TokenPair | None:
        blob = keyring.get_password(self._service, self._username)
        if not blob:
            return None
        try:
            return TokenPair.model_validate_json(blob)
        except (json.JSONDecodeError, pydantic.ValidationError) as e:
            log.warning("keyring data malformed, ignoring: %s", e)
            return None

    def save(self, token_pair: TokenPair) -> None:
        blob = token_pair.model_dump_json(by_alias=True)
        keyring.set_password(self._service, self._username, blob)

    def clear(self) -> None:
        try:
            keyring.delete_password(self._service, self._username)
        except keyring.errors.PasswordDeleteError:
            pass


class FileTokenStore:
    def __init__(self, path: Path):
        self._path = path

    def load(self) -> TokenPair | None:
        if not self._path.exists():
            return None
        try:
            with open(self._path, encoding="utf-8") as f:
                blob = f.read()
            return TokenPair.model_validate_json(blob)
        except (json.JSONDecodeError, pydantic.ValidationError, OSError) as e:
            log.warning("token file unreadable, ignoring: %s", e)
            return None

    def save(self, token_pair: TokenPair) -> None:
        self._path.parent.mkdir(mode=0o700, exist_ok=True)
        tmp_path = self._path.with_suffix(self._path.suffix + ".tmp")

        fd = os.open(tmp_path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            f.write(token_pair.model_dump_json(by_alias=True))

        os.replace(tmp_path, self._path)

    def clear(self) -> None:
        try:
            self._path.unlink()
        except FileNotFoundError:
            pass


def create_token_store(fallback_path: Path) -> TokenStore:
    if _keyring_available():
        log.info("Using OS keyring for token storage")
        return KeyringTokenStore()

    log.warning(
        "OS keyring unavailable; falling back to file storage at %s ",
        fallback_path,
    )

    return FileTokenStore(fallback_path)


def _keyring_available() -> bool:
    try:
        keyring.get_password(KEYRING_SERVICE, "__probe__")
        return True

    except keyring.errors.KeyringError as e:
        log.debug("keyring probe failed: %s", e)
        return False

    except Exception as e:
        log.debug("keyring probe raised unexpected error: %s", e)
        return False
