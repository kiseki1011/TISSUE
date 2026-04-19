from tissue.api.client import TissueClient
from tissue.models.auth import SignupRequest


class MemberAPI:
    def __init__(self, client: TissueClient):
        self.client = client

    async def signup(
        self,
        email: str,
        username: str,
        name: str,
        password: str,
        signup_token: str,
    ) -> bool:
        payload = SignupRequest(
            email=email,
            username=username,
            name=name,
            password=password,
            signup_token=signup_token,
        ).model_dump(by_alias=True)
        try:
            resp = await self.client.request(
                "POST", "/api/v1/members/signup", json=payload, authenticated=False
            )
            return resp.status_code == 201
        except Exception:
            return False

    async def request_verification(self, email: str) -> dict:
        try:
            resp = await self.client.request(
                "POST",
                "/api/v1/members/signup:requestVerification",
                json={"email": email},
                authenticated=False,
            )
            result = {"status": resp.status_code, "verificationId": None}
            if resp.status_code == 200:
                result["verificationId"] = resp.json().get("verificationId")
            return result
        except Exception:
            return {"status": 500, "verificationId": None}

    async def get_verification_status(self, verification_id: str) -> str | None:
        try:
            resp = await self.client.request(
                "GET",
                f"/api/v1/members/signup/status/{verification_id}",
                authenticated=False,
            )
            if resp.status_code == 200:
                data = resp.json()
                if data.get("status") == "VERIFIED":
                    return data.get("signupToken")
            return None
        except Exception:
            return None

    async def check_email_availability(self, email: str) -> int:
        try:
            resp = await self.client.request(
                "GET",
                "/api/v1/members:checkEmail",
                params={"email": email},
                authenticated=False,
            )
            return resp.status_code
        except Exception:
            return 500

    async def check_username_availability(self, username: str) -> bool:
        try:
            resp = await self.client.request(
                "GET",
                "/api/v1/members:checkUsername",
                params={"username": username},
                authenticated=False,
            )
            return resp.status_code == 204
        except Exception:
            return False
