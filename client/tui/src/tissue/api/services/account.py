from __future__ import annotations

from typing import TYPE_CHECKING

import httpx

from tissue.api.errors import TissueApiError, translate
from tissue.api.generated.exceptions import ApiException
from tissue.api.generated.models.email_verification_request import (
    EmailVerificationRequest,
)
from tissue.api.generated.models.member_profile import MemberProfile
from tissue.api.generated.models.restore_member_request import RestoreMemberRequest
from tissue.api.generated.models.signup_member_request import SignupMemberRequest
from tissue.api.generated.models.update_member_name_request import (
    UpdateMemberNameRequest,
)
from tissue.api.generated.models.update_member_password_request import (
    UpdateMemberPasswordRequest,
)
from tissue.api.generated.models.update_member_username_request import (
    UpdateMemberUsernameRequest,
)
from tissue.api.generated.models.verification_status import VerificationStatus
from tissue.api.generated.models.withdraw_member_request import WithdrawMemberRequest

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

    async def update_name(self, new_name: str) -> None:
        """Update the current member's display name and refresh the cache."""
        request = UpdateMemberNameRequest(newName=new_name)
        await self._client._call_with_retry(
            self._client.member_profile_api.update_member_name, request
        )
        if self._profile is not None:
            self._profile = self._profile.model_copy(update={"name": new_name})

    async def update_username(self, new_username: str) -> None:
        """Update the current member's username and refresh the cache."""
        request = UpdateMemberUsernameRequest(newUsername=new_username)
        await self._client._call_with_retry(
            self._client.member_account_api.update_member_username, request
        )
        if self._profile is not None:
            self._profile = self._profile.model_copy(update={"username": new_username})

    async def update_password(
        self, *, original_password: str, new_password: str
    ) -> None:
        request = UpdateMemberPasswordRequest(
            originalPassword=original_password, newPassword=new_password
        )
        await self._client._call_with_retry(
            self._client.member_account_api.update_member_password, request
        )

    async def withdraw(self, password: str) -> None:
        request = WithdrawMemberRequest(password=password)
        await self._client._call_with_retry(
            self._client.member_account_api.withdraw_member, request
        )
        self._profile = None

    async def restore(self, identifier: str, password: str) -> None:
        request = RestoreMemberRequest(identifier=identifier, password=password)
        try:
            await self._client.member_account_api.restore_member(request)
        except (ApiException, httpx.HTTPError) as e:
            raise translate(e) from e
