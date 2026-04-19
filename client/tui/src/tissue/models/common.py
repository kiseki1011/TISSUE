from pydantic import BaseModel, Field


class ErrorResponse(BaseModel):
    """RFC 7807 Problem Details"""

    title: str
    status: int
    detail: str
    instance: str
    occurred_at: str | None = Field(default=None, alias="occurredAt")
    model_config = {"populate_by_name": True}
