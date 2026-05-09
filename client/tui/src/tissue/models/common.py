from typing import Any

from pydantic import BaseModel, Field


class ErrorResponse(BaseModel):
    """RFC 7807 Problem Details"""

    title: str
    status: int
    detail: str | None = None
    instance: str | None = None
    occurred_at: str | None = Field(default=None, alias="occurredAt")
    model_config = {"populate_by_name": True, "extra": "allow"}

    @property
    def extras(self) -> dict[str, Any]:
        return self.model_extra or {}
