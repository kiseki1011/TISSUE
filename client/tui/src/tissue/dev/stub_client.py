import asyncio
import json
import uuid
from datetime import datetime

import httpx

from tissue.api.client import TissueClient
from tissue.dev.fixtures import (
    STUB_ACCOUNTS,
    STUB_REGISTERED_URLS,
    STUB_SYSTEM_INFO,
    STUB_TAKEN_EMAILS,
    STUB_TAKEN_USERNAMES,
)

LATENCY_SECONDS = 0.2
VERIFICATION_DELAY_SECONDS = 2.0

_verifications: dict[str, dict] = {}


def _response(status: int, body: dict | None = None) -> httpx.Response:
    request = httpx.Request("STUB", "http://stub")
    if body is None:
        return httpx.Response(status, request=request)
    return httpx.Response(
        status,
        request=request,
        content=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )


def _dispatch(method: str, path: str, kwargs: dict) -> httpx.Response:
    json_body = kwargs.get("json") or {}
    params = kwargs.get("params") or {}

    if method == "GET" and path == "/api/v1/system-info":
        return _response(200, STUB_SYSTEM_INFO.model_dump(by_alias=True))

    if method == "POST" and path == "/api/v1/auth/login":
        email = json_body.get("loginEmail")
        password = json_body.get("password")
        account = STUB_ACCOUNTS.get(email)
        if account and account["password"] == password:
            return _response(
                200,
                {
                    "accessToken": account["access_token"],
                    "refreshToken": account["refresh_token"],
                },
            )
        return _response(401)

    if method == "POST" and path == "/api/v1/auth/logout":
        return _response(204)

    if method == "POST" and path == "/api/v1/auth/token:refresh":
        refresh = json_body.get("refreshToken", "")
        for account in STUB_ACCOUNTS.values():
            if account["refresh_token"] == refresh:
                return _response(
                    200,
                    {
                        "accessToken": account["access_token"],
                        "refreshToken": account["refresh_token"],
                    },
                )
        return _response(401)

    if method == "GET" and path == "/api/v1/members:checkEmail":
        email = params.get("email", "")
        return _response(409 if email in STUB_TAKEN_EMAILS else 204)

    if method == "GET" and path == "/api/v1/members:checkUsername":
        username = params.get("username", "")
        return _response(409 if username in STUB_TAKEN_USERNAMES else 204)

    if method == "POST" and path == "/api/v1/members/signup:requestVerification":
        email = json_body.get("email", "")
        ver_id = f"ver_{uuid.uuid4().hex[:8]}"
        _verifications[ver_id] = {"email": email, "requested_at": datetime.now()}
        return _response(200, {"verificationId": ver_id})

    if method == "GET" and path.startswith("/api/v1/members/signup/status/"):
        ver_id = path.rsplit("/", 1)[-1]
        record = _verifications.get(ver_id)
        if not record:
            return _response(404)
        elapsed = (datetime.now() - record["requested_at"]).total_seconds()
        if elapsed >= VERIFICATION_DELAY_SECONDS:
            return _response(
                200,
                {"status": "VERIFIED", "signupToken": f"signup_token_{ver_id}"},
            )
        return _response(200, {"status": "PENDING"})

    if method == "POST" and path == "/api/v1/members/signup":
        email = json_body.get("email", "")
        if email in STUB_ACCOUNTS or email in STUB_TAKEN_EMAILS:
            return _response(409)
        STUB_ACCOUNTS[email] = {
            "password": json_body.get("password"),
            "access_token": f"stub_access_{email}",
            "refresh_token": f"stub_refresh_{email}",
        }
        STUB_TAKEN_EMAILS.add(email)
        return _response(201)

    return _response(404)


class StubTissueClient(TissueClient):
    async def request(
        self,
        method: str,
        path: str,
        *,
        authenticated: bool = True,
        **kwargs,
    ) -> httpx.Response:
        await asyncio.sleep(LATENCY_SECONDS)
        if self.base_url not in STUB_REGISTERED_URLS:
            raise httpx.ConnectError(f"Cannot connect to {self.base_url}")
        return _dispatch(method, path, kwargs)
