import logging

import httpx

from tissue.api.errors import NotTissueServer, TissueApiError, translate
from tissue.api.generated.api.authentication_api import AuthenticationApi
from tissue.api.generated.api.member_account_api import MemberAccountApi
from tissue.api.generated.api.member_signup_api import MemberSignupApi
from tissue.api.generated.api.system_info_api import SystemInfoApi
from tissue.api.generated.api_client import ApiClient
from tissue.api.generated.configuration import Configuration
from tissue.api.generated.exceptions import ApiException
from tissue.api.generated.models.email_verification_request import (
    EmailVerificationRequest,
)
from tissue.api.generated.models.login_request import LoginRequest
from tissue.api.generated.models.signup_member_request import SignupMemberRequest
from tissue.api.generated.models.system_info_details import SystemInfoDetails
from tissue.api.generated.models.verification_status import VerificationStatus
from tissue.models.auth import TokenPair

log = logging.getLogger(__name__)


class TissueClient:
    def __init__(self, host: str) -> None:
        normalized = host.rstrip("/")
        self._config = Configuration(host=normalized)
        self._api_client = ApiClient(configuration=self._config)
        self._system_info_api: SystemInfoApi | None = None
        self._auth_api: AuthenticationApi | None = None
        self._signup_api: MemberSignupApi | None = None
        self._member_account_api: MemberAccountApi | None = None

    @property
    def host(self) -> str:
        return self._config.host

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

    def set_token(self, access_token: str) -> None:
        self._config.access_token = access_token

    def clear_token(self) -> None:
        self._config.access_token = None

    async def ping(self) -> SystemInfoDetails:
        """Probe /system-info. Raises if unreachable or not a Tissue server."""
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
        self.set_token(token.access_token)
        return token

    async def signup(
        self,
        *,
        username: str,
        name: str,
        password: str,
        email: str | None = None,
        verified_token: str | None = None,
    ) -> None:
        # use model_construct to bypass generated field_validators (the `name`
        # validator uses `\p{L}` which is unsupported by python's `re` module).
        # backend re-validates with java regex.
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
