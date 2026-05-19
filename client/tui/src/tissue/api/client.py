import logging
from collections.abc import Awaitable, Callable
from typing import TypeVar

import httpx

from tissue.api.errors import NotTissueServer, TissueApiError, translate
from tissue.api.generated.api.authentication_api import AuthenticationApi
from tissue.api.generated.api.invitation_api import InvitationApi
from tissue.api.generated.api.member_account_api import MemberAccountApi
from tissue.api.generated.api.member_profile_api import MemberProfileApi
from tissue.api.generated.api.member_signup_api import MemberSignupApi
from tissue.api.generated.api.system_info_api import SystemInfoApi
from tissue.api.generated.api.workspace_api import WorkspaceApi
from tissue.api.generated.api.workspace_participation_api import (
    WorkspaceParticipationApi,
)
from tissue.api.generated.api_client import ApiClient
from tissue.api.generated.configuration import Configuration
from tissue.api.generated.exceptions import ApiException
from tissue.api.generated.models.refresh_token_request import RefreshTokenRequest
from tissue.api.generated.models.system_info_details import SystemInfoDetails
from tissue.api.services.account import AccountService
from tissue.api.services.auth import AuthService
from tissue.api.services.invitations import InvitationService
from tissue.api.services.workspaces import WorkspaceService
from tissue.auth.token_store import TokenStore, TokenStoreError
from tissue.models.auth import TokenPair

log = logging.getLogger(__name__)

T = TypeVar("T")


class TissueClient:
    """Facade over the generated API.

    TissueClient is responsible for tokens, refresh, retry, ping, lifecycle.
    It delegates domain operations to services exposed as fields.
    """

    def __init__(self, host: str, token_store: TokenStore | None = None) -> None:
        normalized = host.rstrip("/")
        self._config = Configuration(host=normalized)
        self._api_client = ApiClient(configuration=self._config)
        self._token_store = token_store
        self._token_pair: TokenPair | None = None

        # Lazily-built generated API singletons
        self._system_info_api: SystemInfoApi | None = None
        self._auth_api: AuthenticationApi | None = None
        self._signup_api: MemberSignupApi | None = None
        self._member_account_api: MemberAccountApi | None = None
        self._member_profile_api: MemberProfileApi | None = None
        self._workspace_api: WorkspaceApi | None = None
        self._workspace_participation_api: WorkspaceParticipationApi | None = None
        self._invitation_api: InvitationApi | None = None

        # Domain services
        self.auth = AuthService(self)
        self.account = AccountService(self)
        self.workspaces = WorkspaceService(self)
        self.invitations = InvitationService(self)

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
    def workspace_api(self) -> WorkspaceApi:
        if self._workspace_api is None:
            self._workspace_api = WorkspaceApi(self._api_client)
        return self._workspace_api

    @property
    def workspace_participation_api(self) -> WorkspaceParticipationApi:
        if self._workspace_participation_api is None:
            self._workspace_participation_api = WorkspaceParticipationApi(
                self._api_client
            )
        return self._workspace_participation_api

    @property
    def invitation_api(self) -> InvitationApi:
        if self._invitation_api is None:
            self._invitation_api = InvitationApi(self._api_client)
        return self._invitation_api

    def set_tokens(self, token_pair: TokenPair) -> None:
        """Store the token pair and configure the access token for outgoing requests."""
        self._token_pair = token_pair
        self._config.access_token = token_pair.access_token
        self._persist_tokens()

    def clear_tokens(self) -> None:
        self._token_pair = None
        self.account._set_cached_profile(None)
        self.workspaces._set_cached(None)
        self.invitations._set_cached(None)
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
        """Wrapper for authenticated API calls. On 401, refresh tokens once and retry.

        Use this to wrap any endpoint that requires authentication. Public endpoints
        should call generated APIs directly.
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

        # TODO: check minRequiredTuiVersion for server-client compatibility

        return info

    async def close(self) -> None:
        await self._api_client.close()

    async def _prefetch_user_context(self) -> None:
        """Fetch profile, workspaces, and invitations in parallel.

        Called right after login so the post-login router can branch without
        another round-trip. Per-endpoint failures are logged at DEBUG since
        they may be expected (e.g. first attempt in restore_session with an
        expired access token).
        """
        import asyncio

        profile, workspaces, invitations = await asyncio.gather(
            self.member_profile_api.get_my_profile(),
            self.workspace_api.list_my_workspaces(),
            self.invitation_api.list_my_invitations(),
            return_exceptions=True,
        )
        if isinstance(profile, BaseException):
            log.debug("Failed to prefetch member profile: %s", profile)
        else:
            self.account._set_cached_profile(profile)
        if isinstance(workspaces, BaseException):
            log.debug("Failed to prefetch workspaces: %s", workspaces)
        else:
            self.workspaces._set_cached(workspaces)
        if isinstance(invitations, BaseException):
            log.debug("Failed to prefetch invitations: %s", invitations)
        else:
            self.invitations._set_cached(invitations)
