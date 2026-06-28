from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.screens.form_helpers import set_field_status as set_field_status

if TYPE_CHECKING:
    from tissue.api.errors import TissueApiError
    from tissue.app import TissueApp


def is_oidc_mode(app: TissueApp) -> bool:
    """True when the server runs in OIDC mode, so there is no local password."""
    info = app.system_info
    setup = info.setup if info is not None else None
    return bool(setup and (setup.auth_mode or "").upper() == "OIDC")


def failure_reason(
    error: TissueApiError, *, password_message: str = "Password is incorrect"
) -> str:
    """A short, user-facing reason for a failed account action."""
    if error.title in ("INVALID_PASSWORD", "PASSWORD_MISMATCH"):
        return password_message
    return error.detail or error.title or str(error)
