from __future__ import annotations

import asyncio
import logging
import time
from collections.abc import Callable
from typing import TYPE_CHECKING

import httpx

from tissue.api.errors import InvalidCredentials, TissueApiError

if TYPE_CHECKING:
    from tissue.api.client import TissueClient

log = logging.getLogger(__name__)

STATE_CONNECTED = "connected"
STATE_CONNECTING = "connecting"
STATE_DISCONNECTED = "disconnected"

_STREAM_PATH = "/api/v1/events/stream"
_INITIAL_BACKOFF = 1.0
_MAX_BACKOFF = 30.0
# A stream must stay up at least this long to count as healthy
_HEALTHY_SECONDS = 5.0
# Read timeout sits above the server's 15s heartbeat: a healthy stream always
# delivers something within the window, so a read timeout means the connection is
# dead (e.g. a half-open TCP) and we should reconnect.
_TIMEOUT = httpx.Timeout(connect=10.0, read=40.0, write=10.0, pool=10.0)


class _Unauthorized(Exception):
    """The stream was refused with 401; the access token needs refreshing."""


class RealtimeConsumer:
    """Owns the SSE connection loop for one logged-in session.

    Run as an app-scoped worker; cancel the worker to stop it. `on_state` is
    called with one of the STATE_* values whenever the connection changes.
    """

    def __init__(
        self, client: TissueClient, *, on_state: Callable[[str], None]
    ) -> None:
        self._client = client
        self._on_state = on_state

    async def run(self) -> None:
        """Connect, read, and reconnect until the worker is cancelled."""
        try:
            await self._loop()
        finally:
            self._on_state(STATE_DISCONNECTED)

    async def _loop(self) -> None:
        backoff = _INITIAL_BACKOFF
        while True:
            self._on_state(STATE_CONNECTING)
            started = time.monotonic()
            try:
                await self._stream_once()
            except asyncio.CancelledError:
                raise
            except _Unauthorized:
                if not await self._refresh():
                    return  # session truly expired
            except Exception as e:
                # A background loop must never die on a transient network error
                log.debug("Realtime: stream error: %s", e)
            # The stream is down
            self._on_state(STATE_CONNECTING)
            if time.monotonic() - started >= _HEALTHY_SECONDS:
                backoff = _INITIAL_BACKOFF
            await asyncio.sleep(backoff)
            backoff = min(backoff * 2, _MAX_BACKOFF)

    async def _stream_once(self) -> None:
        token = self._client.access_token
        if token is None:
            raise _Unauthorized()
        headers = {
            "Authorization": f"Bearer {token}",
            "Accept": "text/event-stream",
        }
        async with self._client.stream(
            "GET", _STREAM_PATH, headers=headers, timeout=_TIMEOUT
        ) as response:
            if response.status_code == 401:
                raise _Unauthorized()
            if response.status_code >= 400:
                raise RuntimeError(
                    f"realtime stream returned HTTP {response.status_code}"
                )
            self._on_state(STATE_CONNECTED)
            fields: dict[str, str] = {}
            async for line in response.aiter_lines():
                if line == "":
                    self._dispatch(fields)
                    fields = {}
                elif not line.startswith(":"):
                    self._collect(line, fields)

    def _collect(self, line: str, fields: dict[str, str]) -> None:
        """Accumulate one SSE field line into the current event."""
        field, _, value = line.partition(":")
        if value.startswith(" "):
            value = value[1:]
        if field == "data" and "data" in fields:
            fields["data"] = f"{fields['data']}\n{value}"
        elif field in ("event", "data", "id"):
            fields[field] = value

    def _dispatch(self, fields: dict[str, str]) -> None:
        if not fields:
            return
        # Phase 2 only consumes the stream to keep it alive and drive the header
        # indicator; turning events into targeted UI patches comes in a later step.
        log.debug("Realtime event: %s", fields.get("event"))

    async def _refresh(self) -> bool:
        """Refresh the token after a 401.

        Returns False only when the session is truly gone (a real 401 — the refresh
        token is invalid/expired). A transient network/5xx failure returns True so the
        loop keeps retrying with backoff instead of tearing the session down.
        """
        token_at_call = self._client.access_token
        try:
            await self._client.refresh_session(token_at_call)
            return True
        except InvalidCredentials:
            return False  # refresh token rejected → session is over
        except TissueApiError as e:
            log.debug("Realtime: transient refresh error: %s", e)
            return True
        except Exception as e:
            log.debug("Realtime: token refresh failed: %s", e)
            return True
