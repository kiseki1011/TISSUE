import logging

from tissue.api.client import TissueClient
from tissue.api.errors import ApiResponseError, ApiSchemaError
from tissue.models.auth import SignupRequest

log = logging.getLogger(__name__)


class MemberAPI:
    def __init__(self, client: TissueClient):
        self.client = client

    async def signup(
        self,
        email: str,
        username: str,
        name: str,
        password: str,
        verified_token: str,
    ) -> None:
        payload = SignupRequest(
            email=email,
            username=username,
            name=name,
            password=password,
            verified_token=verified_token,
        ).model_dump(by_alias=True)
        resp = await self.client.request(
            "POST", "/api/v1/members/signup", json=payload, authenticated=False
        )
        if resp.status_code != 201:
            raise ApiResponseError.from_response(resp)

    async def request_verification(self, email: str) -> str:
        resp = await self.client.request(
            "POST",
            "/api/v1/members/signup:requestVerification",
            json={"email": email},
            authenticated=False,
        )
        if resp.status_code != 200:
            raise ApiResponseError.from_response(resp)
        verification_id = resp.json().get("verificationId")
        if not verification_id:
            raise ApiSchemaError("missing verificationId")
        return verification_id

    async def get_verification_status(self, verification_id: str) -> str | None:
        resp = await self.client.request(
            "GET",
            f"/api/v1/members/signup/status/{verification_id}",
            authenticated=False,
        )
        if resp.status_code != 200:
            raise ApiResponseError.from_response(resp)
        data = resp.json()
        if data.get("status") == "VERIFIED":
            return data.get("verifiedToken")
        return None

    async def check_email_availability(self, email: str) -> bool:
        resp = await self.client.request(
            "GET",
            "/api/v1/members:checkEmail",
            params={"email": email},
            authenticated=False,
        )
        if resp.status_code == 204:
            return True
        if resp.status_code == 409:
            return False
        raise ApiResponseError.from_response(resp)

    async def check_username_availability(self, username: str) -> bool:
        resp = await self.client.request(
            "GET",
            "/api/v1/members:checkUsername",
            params={"username": username},
            authenticated=False,
        )
        if resp.status_code == 204:
            return True
        if resp.status_code == 409:
            return False
        raise ApiResponseError.from_response(resp)
