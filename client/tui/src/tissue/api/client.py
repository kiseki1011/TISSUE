import logging
from collections.abc import Awaitable, Callable
from typing import TypeVar

import httpx

from tissue.api.errors import NotTissueServer, TissueApiError, translate
from tissue.api.generated.api.authentication_api import AuthenticationApi
from tissue.api.generated.api.member_account_api import MemberAccountApi
from tissue.api.generated.api.member_profile_api import MemberProfileApi
from tissue.api.generated.api.member_signup_api import MemberSignupApi
from tissue.api.generated.api.system_info_api import SystemInfoApi
from tissue.api.generated.api_client import ApiClient
from tissue.api.generated.configuration import Configuration
from tissue.api.generated.exceptions import ApiException
from tissue.api.generated.models.email_verification_request import (
    EmailVerificationRequest,
)
from tissue.api.generated.models.login_request import LoginRequest
from tissue.api.generated.models.member_profile import MemberProfile
from tissue.api.generated.models.refresh_token_request import RefreshTokenRequest
from tissue.api.generated.models.signup_member_request import SignupMemberRequest
from tissue.api.generated.models.system_info_details import SystemInfoDetails
from tissue.api.generated.models.verification_status import VerificationStatus
from tissue.auth.token_store import TokenStore, TokenStoreError
from tissue.models.auth import TokenPair

log = logging.getLogger(__name__)

T = TypeVar("T")


class TissueClient:
    def __init__(self, host: str, token_store: TokenStore | None = None) -> None:
        normalized = host.rstrip("/")
        self._config = Configuration(host=normalized)
        self._api_client = ApiClient(configuration=self._config)
        self._token_store = token_store
        self._token_pair: TokenPair | None = None
        self._member_profile: MemberProfile | None = None
        self._system_info_api: SystemInfoApi | None = None
        self._auth_api: AuthenticationApi | None = None
        self._signup_api: MemberSignupApi | None = None
        self._member_account_api: MemberAccountApi | None = None
        self._member_profile_api: MemberProfileApi | None = None

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
    def auth(self) -> AuthenticationApi:
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
    def member_profile(self) -> MemberProfile | None:
        """Cached profile of the user"""
        return self._member_profile

    def set_tokens(self, token_pair: TokenPair) -> None:
        """Store the token pair and configure the access token for outgoing requests"""
        self._token_pair = token_pair
        self._config.access_token = token_pair.access_token
        self._persist_tokens()

    def clear_tokens(self) -> None:
        """Clear in-memory tokens and remove the persisted token pair for this host"""
        self._token_pair = None
        self._member_profile = None
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
        """Use current refresh token for a new token pair"""
        if self._token_pair is None:
            raise TissueApiError("No refresh token available")
        request = RefreshTokenRequest(refreshToken=self._token_pair.refresh_token)
        try:
            response = await self.auth.refresh_token(request)
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
            # Anything other than 401 is thrown
            if err.status != 401 or self._token_pair is None:
                raise err from e

        try:
            await self.refresh()
        except TissueApiError:
            # Clear tokens on refresh fail to route the user back to login
            self.clear_tokens()
            raise

        try:
            return await fn(*args, **kwargs)
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

    async def ping(self) -> SystemInfoDetails:
        """Probe system-info. Throws exception if unreachable or not a Tissue server."""
        try:
            info = await self.system_info.get_system_info()
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

        if info.version is None:
            raise NotTissueServer("The tissue server always comes with a version")

        return info

    async def login(self, identifier: str, password: str) -> TokenPair:
        request = LoginRequest(identifier=identifier, password=password)
        try:
            response = await self.auth.login(request)
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

        if response.access_token is None or response.refresh_token is None:
            raise TissueApiError("Server returned incomplete login response")

        token = TokenPair(
            access_token=response.access_token,
            refresh_token=response.refresh_token,
        )
        self.set_tokens(token)
        # Prefetch profile so screens can display user info without an extra round-trip
        try:
            self._member_profile = await self.member_profile_api.get_my_profile()
        except (ApiException, httpx.HTTPError) as e:
            log.warning("Failed to prefetch member profile after login: %s", e)
        return token

    async def logout(self) -> None:
        """Server-side refresh token revoke and clear local state."""
        try:
            await self.auth.logout()
        except (ApiException, httpx.HTTPError) as e:
            log.warning("Logout request failed (clearing local state anyway): %s", e)
        finally:
            self.clear_tokens()

    async def signup(
        self,
        *,
        username: str,
        name: str,
        password: str,
        email: str | None = None,
        verified_token: str | None = None,
    ) -> None:
        request = SignupMemberRequest.model_construct(
            email=email,
            username=username,
            name=name,
            password=password,
            verifiedToken=verified_token,
        )
        try:
            await self.signup_api.signup(request)
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

    async def request_signup_verification(self, email: str) -> str:
        try:
            response = await self.signup_api.request_signup_verification(
                EmailVerificationRequest(email=email)
            )
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

        if response.verification_id is None:
            raise TissueApiError("Server returned no verification id")
        return response.verification_id

    async def check_signup_verification(
        self, verification_id: str
    ) -> VerificationStatus:
        try:
            return await self.signup_api.check_signup_verification(verification_id)
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

    async def check_email_availability(self, email: str) -> bool:
        try:
            await self.member_account_api.check_email_availability(email)
            return True
        except (ApiException, httpx.HTTPError) as e:
            err = translate(e)
            if err.status == 409:
                return False
            raise err from e

    async def check_username_availability(self, username: str) -> bool:
        try:
            await self.member_account_api.check_username_availability(username)
            return True
        except (ApiException, httpx.HTTPError) as e:
            err = translate(e)
            if err.status == 409:
                return False
            raise err from e

    async def close(self) -> None:
        await self._api_client.close()
