from __future__ import annotations

import asyncio
import json
import logging
import time
from collections.abc import Callable
from dataclasses import dataclass
from typing import TYPE_CHECKING

import httpx

from tissue.api.errors import InvalidCredentials, TissueApiError

if TYPE_CHECKING:
    from tissue.api.client import TissueClient

log = logging.getLogger(__name__)

STATE_CONNECTED = "connected"
STATE_CONNECTING = "connecting"
STATE_DISCONNECTED = "disconnected"


@dataclass(frozen=True)
class RealtimeEvent:
    """One parsed message off the realtime SSE stream (backend RealtimeMessage)."""

    category: str
    type: str
    project_key: str
    issue_key: str | None
    actor_member_id: int | None
    data: dict[str, object]


_STREAM_PATH = "/api/v1/events/stream"
_INITIAL_BACKOFF = 1.0
_MAX_BACKOFF = 30.0
_HEALTHY_SECONDS = 5.0
# Read timeout sits above the 15s heartbeat so a silent socket reads as dead.
_TIMEOUT = httpx.Timeout(connect=10.0, read=40.0, write=10.0, pool=10.0)


class _Unauthorized(Exception):
    """The stream was refused with 401."""


class RealtimeConsumer:
    """Owns the SSE connection loop for one logged-in session.

    Run as an app-scoped worker. Cancel it to stop.
    """

    def __init__(
        self,
        client: TissueClient,
        *,
        on_state: Callable[[str], None],
        on_event: Callable[[RealtimeEvent], None] | None = None,
    ) -> None:
        self._client = client
        self._on_state = on_state
        self._on_event = on_event

    async def run(self) -> None:
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
                log.debug("Realtime: stream error: %s", e)
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
        field, _, value = line.partition(":")
        if value.startswith(" "):
            value = value[1:]
        if field == "data" and "data" in fields:
            fields["data"] = f"{fields['data']}\n{value}"
        elif field in ("event", "data", "id"):
            fields[field] = value

    def _dispatch(self, fields: dict[str, str]) -> None:
        raw = fields.get("data")
        if self._on_event is None or not raw:
            return
        try:
            payload = json.loads(raw)
        except ValueError:
            log.debug("Realtime: unparseable event payload")
            return
        if not isinstance(payload, dict):
            return
        data = payload.get("data")
        self._on_event(
            RealtimeEvent(
                category=fields.get("event", ""),
                type=str(payload.get("type") or ""),
                project_key=str(payload.get("projectKey") or ""),
                issue_key=payload.get("issueKey"),
                actor_member_id=payload.get("actorMemberId"),
                data=data if isinstance(data, dict) else {},
            )
        )

    async def _refresh(self) -> bool:
        """Refresh the token after a 401.

        Returns False only when the refresh token is rejected (session truly gone). A
        transient network or 5xx failure returns True so the loop keeps retrying.
        """
        token_at_call = self._client.access_token
        try:
            await self._client.refresh_session(token_at_call)
            return True
        except InvalidCredentials:
            return False
        except TissueApiError as e:
            log.debug("Realtime: transient refresh error: %s", e)
            return True
        except Exception as e:
            log.debug("Realtime: token refresh failed: %s", e)
            return True
