import logging
from collections.abc import Awaitable, Callable
from typing import TypeVar

import httpx

from tissue.api.errors import NotTissueServer, TissueApiError, translate
from tissue.api.generated.api.authentication_api import AuthenticationApi
from tissue.api.generated.api.member_account_api import MemberAccountApi
from tissue.api.generated.api.member_profile_api import MemberProfileApi
from tissue.api.generated.api.member_signup_api import MemberSignupApi
from tissue.api.generated.api.project_api import ProjectApi
from tissue.api.generated.api.system_info_api import SystemInfoApi
from tissue.api.generated.api_client import ApiClient
from tissue.api.generated.configuration import Configuration
from tissue.api.generated.exceptions import ApiException
from tissue.api.generated.models.refresh_token_request import RefreshTokenRequest
from tissue.api.generated.models.system_info_details import SystemInfoDetails
from tissue.api.services.account import AccountService
from tissue.api.services.auth import AuthService
from tissue.api.services.projects import ProjectService
from tissue.auth.token_store import TokenStore, TokenStoreError
from tissue.models.auth import TokenPair

log = logging.getLogger(__name__)

T = TypeVar("T")


class TissueClient:
    """Wrapper over the generated API.

    `TissueClient` is responsible for tokens, refresh, retry, ping, lifecycle.
    It delegates domain operations to services exposed as fields.
    """

    def __init__(self, host: str, token_store: TokenStore | None = None) -> None:
        normalized = host.rstrip("/")
        self._config = Configuration(host=normalized)
        self._api_client = ApiClient(configuration=self._config)
        self._token_store = token_store
        self._token_pair: TokenPair | None = None

        self._system_info_api: SystemInfoApi | None = None
        self._auth_api: AuthenticationApi | None = None
        self._signup_api: MemberSignupApi | None = None
        self._member_account_api: MemberAccountApi | None = None
        self._member_profile_api: MemberProfileApi | None = None
        self._project_api: ProjectApi | None = None

        # Domain services
        self.auth = AuthService(self)
        self.account = AccountService(self)
        self.projects = ProjectService(self)

    @property
    def host(self) -> str:
        return self._config.host

    @property
    def is_authenticated(self) -> bool:
        return self._token_pair is not None

    @property
    def system_info(self) -> SystemInfoApi:
        if self._system_info_api is None:
            self._system_info_api = SystemInfoApi(self._api_client)
        return self._system_info_api

    @property
    def auth_api(self) -> AuthenticationApi:
        if self._auth_api is None:
            self._auth_api = AuthenticationApi(self._api_client)
        return self._auth_api

    @property
    def signup_api(self) -> MemberSignupApi:
        if self._signup_api is None:
            self._signup_api = MemberSignupApi(self._api_client)
        return self._signup_api

    @property
    def member_account_api(self) -> MemberAccountApi:
        if self._member_account_api is None:
            self._member_account_api = MemberAccountApi(self._api_client)
        return self._member_account_api

    @property
    def member_profile_api(self) -> MemberProfileApi:
        if self._member_profile_api is None:
            self._member_profile_api = MemberProfileApi(self._api_client)
        return self._member_profile_api

    @property
    def project_api(self) -> ProjectApi:
        if self._project_api is None:
            self._project_api = ProjectApi(self._api_client)
        return self._project_api

    def set_tokens(self, token_pair: TokenPair) -> None:
        """Store the token pair and configure the access token for outgoing requests."""
        self._token_pair = token_pair
        self._config.access_token = token_pair.access_token
        self._persist_tokens()

    def clear_tokens(self) -> None:
        self._token_pair = None
        self.account._set_cached_profile(None)
        self._config.access_token = None
        if self._token_store is not None:
            self._token_store.clear(self.host)

    def _persist_tokens(self) -> None:
        if self._token_store is None or self._token_pair is None:
            return
        try:
            self._token_store.save(self.host, self._token_pair)
        except TokenStoreError as e:
            log.warning("Failed to persist tokens for %s: %s", self.host, e)

    async def refresh(self) -> None:
        """Use current refresh token for a new token pair."""
        if self._token_pair is None:
            raise TissueApiError("No refresh token available")
        request = RefreshTokenRequest(refreshToken=self._token_pair.refresh_token)
        try:
            response = await self.auth_api.refresh_token(request)
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

        if response.access_token is None or response.refresh_token is None:
            raise TissueApiError("Server returned incomplete refresh response")

        self.set_tokens(
            TokenPair(
                access_token=response.access_token,
                refresh_token=response.refresh_token,
            )
        )

    async def _call_with_retry(
        self, fn: Callable[..., Awaitable[T]], *args, **kwargs
    ) -> T:
        """Wrapper for authenticated API calls.

        On 401, refresh tokens once and retry. Use this to wrap any endpoint that
        requires authentication. Public endpoints should call generated APIs directly.
        """
        try:
            return await fn(*args, **kwargs)
        except (ApiException, httpx.HTTPError) as e:
            err = translate(e)
            if err.status != 401 or self._token_pair is None:
                raise err from e

        try:
            await self.refresh()
        except TissueApiError:
            self.clear_tokens()
            raise

        try:
            return await fn(*args, **kwargs)
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

    async def ping(self) -> SystemInfoDetails:
        try:
            info = await self.system_info.get_system_info()
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

        if info.version is None:
            raise NotTissueServer("The tissue server always comes with a version")

        return info

    async def close(self) -> None:
        await self._api_client.close()

    async def _prefetch_user_context(self) -> None:
        """Fetch the member profile so the post-login router can branch without
        another round trip.
        """
        try:
            profile = await self.member_profile_api.get_my_profile()
        except (ApiException, httpx.HTTPError) as e:
            log.debug("Failed to prefetch member profile: %s", e)
            return
        self.account._set_cached_profile(profile)
