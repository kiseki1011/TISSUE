from __future__ import annotations

import logging
from typing import TYPE_CHECKING

import httpx

from tissue.api.errors import TissueApiError, translate
from tissue.api.generated.exceptions import ApiException
from tissue.api.generated.models.login_request import LoginRequest
from tissue.models.auth import TokenPair

if TYPE_CHECKING:
    from tissue.api.client import TissueClient

log = logging.getLogger(__name__)


class AuthService:
    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def login(self, identifier: str, password: str) -> TokenPair:
        request = LoginRequest(identifier=identifier, password=password)
        try:
            response = await self._client.auth_api.login(request)
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

        if response.access_token is None or response.refresh_token is None:
            raise TissueApiError("Server returned incomplete login response")

        token = TokenPair(
            access_token=response.access_token,
            refresh_token=response.refresh_token,
        )
        self._client.set_tokens(token)
        await self._client._prefetch_user_context()
        return token

    async def restore_session(self, token_pair: TokenPair) -> bool:
        """Restore an authenticated session from a previously stored token.

        Tries the stored access token first. If prefetch fails (probably a
        401 because access tokens are short-lived), refresh once with the
        stored refresh token and try prefetch again.
        """
        self._client.set_tokens(token_pair)

        await self._client._prefetch_user_context()
        if self._client.account.cached_profile is not None:
            return True

        try:
            await self._client.refresh()
        except TissueApiError as e:
            log.debug("Refresh during restore_session failed: %s", e)
            return False

        await self._client._prefetch_user_context()
        return self._client.account.cached_profile is not None

    async def logout(self) -> None:
        try:
            await self._client.auth_api.logout()
        except (ApiException, httpx.HTTPError) as e:
            log.warning("Logout request failed (clearing local state anyway): %s", e)
        finally:
            self._client.clear_tokens()
