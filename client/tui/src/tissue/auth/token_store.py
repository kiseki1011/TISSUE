import json
import logging
import os
from pathlib import Path
from typing import Any, Protocol

import keyring
import keyring.errors
import pydantic

from tissue.models.auth import TokenPair
from tissue.paths import credentials_path

log = logging.getLogger(__name__)

KEYRING_SERVICE = "tissue"


class TokenStoreError(Exception):
    """Failed to persist token to the underlying storage"""

    pass


class TokenStore(Protocol):
    def load(self, server_url: str) -> TokenPair | None: ...

    def save(self, server_url: str, token_pair: TokenPair) -> None: ...

    def clear(self, server_url: str) -> None: ...


class KeyringTokenStore:
    def __init__(self, service: str = KEYRING_SERVICE):
        self._service = service

    def load(self, server_url: str) -> TokenPair | None:
        try:
            blob = keyring.get_password(self._service, server_url)
        except keyring.errors.KeyringError as e:
            log.warning("keyring access failed for %s: %s", server_url, e)
            return None

        if not blob:
            return None
        try:
            return TokenPair.model_validate_json(blob)
        except (json.JSONDecodeError, pydantic.ValidationError) as e:
            log.warning("keyring data malformed for %s, ignoring: %s", server_url, e)
            return None

    def save(self, server_url: str, token_pair: TokenPair) -> None:
        blob = token_pair.model_dump_json()
        try:
            keyring.set_password(self._service, server_url, blob)
        except keyring.errors.KeyringError as e:
            log.error("Failed to save tokens to keyring for %s: %s", server_url, e)
            raise TokenStoreError(str(e)) from e
        log.debug("Saved tokens to keyring for %s", server_url)

    def clear(self, server_url: str) -> None:
        try:
            keyring.delete_password(self._service, server_url)
        except keyring.errors.PasswordDeleteError:
            pass  # clear should be idempotent
        log.debug("Cleared tokens for %s", server_url)


class FileTokenStore:
    def __init__(self, path: Path):
        self._path = path

    def load(self, server_url: str) -> TokenPair | None:
        all_tokens = self._load_all()
        entry = all_tokens.get(server_url)
        if not entry:
            return None
        try:
            return TokenPair.model_validate(entry)
        except pydantic.ValidationError as e:
            log.warning("token data malformed for %s, ignoring: %s", server_url, e)
            return None

    def save(self, server_url: str, token_pair: TokenPair) -> None:
        all_tokens = self._load_all()
        all_tokens[server_url] = token_pair.model_dump()
        self._save_all(all_tokens)
        log.debug("Saved tokens to %s for %s", self._path, server_url)

    def clear(self, server_url: str) -> None:
        all_tokens = self._load_all()
        if server_url in all_tokens:
            del all_tokens[server_url]
            self._save_all(all_tokens)
        log.debug("Cleared tokens for %s", server_url)

    def _load_all(self) -> dict[str, Any]:
        if not self._path.exists():
            return {}
        try:
            with open(self._path, encoding="utf-8") as f:
                data = json.loads(f.read())
        except (json.JSONDecodeError, OSError) as e:
            log.warning("token file unreadable, ignoring: %s", e)
            return {}

        if not isinstance(data, dict):
            log.warning("token file root is not a dict, ignoring")
            return {}
        return data

    def _save_all(self, data: dict[str, Any]) -> None:
        try:
            self._path.parent.mkdir(mode=0o700, exist_ok=True)
            tmp_path = self._path.with_suffix(self._path.suffix + ".tmp")

            fd = os.open(tmp_path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
            with os.fdopen(fd, "w", encoding="utf-8") as f:
                f.write(json.dumps(data))

            os.replace(tmp_path, self._path)
        except OSError as e:
            log.error("Failed to save tokens to %s: %s", self._path, e)
            raise TokenStoreError(str(e)) from e


def create_token_store() -> TokenStore:
    if _keyring_available():
        log.info("Using OS keyring for token storage")
        return KeyringTokenStore()

    fallback = credentials_path()
    log.warning(
        "OS keyring unavailable; falling back to file storage at %s",
        fallback,
    )
    return FileTokenStore(fallback)


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
