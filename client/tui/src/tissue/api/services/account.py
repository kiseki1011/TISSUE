from __future__ import annotations

from typing import TYPE_CHECKING

import httpx

from tissue.api.errors import TissueApiError, translate
from tissue.api.generated.exceptions import ApiException
from tissue.api.generated.models.email_verification_request import (
    EmailVerificationRequest,
)
from tissue.api.generated.models.member_profile import MemberProfile
from tissue.api.generated.models.signup_member_request import SignupMemberRequest
from tissue.api.generated.models.verification_status import VerificationStatus

if TYPE_CHECKING:
    from tissue.api.client import TissueClient


class AccountService:
    def __init__(self, client: TissueClient) -> None:
        self._client = client
        self._profile: MemberProfile | None = None

    @property
    def cached_profile(self) -> MemberProfile | None:
        return self._profile

    def _set_cached_profile(self, profile: MemberProfile | None) -> None:
        """Set by client during prefetch. Clear on logout."""
        self._profile = profile

    async def check_email_available(self, email: str) -> bool:
        try:
            await self._client.member_account_api.check_email_availability(email)
            return True
        except (ApiException, httpx.HTTPError) as e:
            err = translate(e)
            if err.status == 409:
                return False
            raise err from e

    async def check_username_available(self, username: str) -> bool:
        try:
            await self._client.member_account_api.check_username_availability(username)
            return True
        except (ApiException, httpx.HTTPError) as e:
            err = translate(e)
            if err.status == 409:
                return False
            raise err from e

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
            await self._client.signup_api.signup(request)
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e

    async def request_signup_verification(self, email: str) -> str:
        try:
            response = await self._client.signup_api.request_signup_verification(
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
            return await self._client.signup_api.check_signup_verification(
                verification_id
            )
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e
