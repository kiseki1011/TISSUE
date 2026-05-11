from datetime import datetime

from pydantic import BaseModel, Field


class InvitationSummary(BaseModel):
    invitation_id: int = Field(alias="invitationId")
    workspace_key: str = Field(alias="workspaceKey")
    workspace_name: str = Field(alias="workspaceName")
    project_keys: list[str] = Field(default_factory=list, alias="projectKeys")
    inviter_name: str = Field(alias="inviterName")
    inviter_email: str | None = Field(default=None, alias="inviterEmail")
    status: str
    invited_at: datetime = Field(alias="invitedAt")
    model_config = {"populate_by_name": True}
