from datetime import datetime

from pydantic import BaseModel, Field


class WorkspaceSummary(BaseModel):
    workspace_key: str = Field(alias="workspaceKey")
    name: str
    description: str | None = None
    created_at: datetime = Field(alias="createdAt")
    my_role: str = Field(alias="myRole")
    member_count: int | None = Field(default=None, alias="memberCount")
    joined_at: datetime | None = Field(default=None, alias="joinedAt")
    archived: bool = False
    deleted: bool = False
    model_config = {"populate_by_name": True}


class CreateWorkspaceRequest(BaseModel):
    workspace_key: str = Field(alias="workspaceKey")
    name: str
    description: str | None = None
    model_config = {"populate_by_name": True}


class WorkspaceCreateResponse(BaseModel):
    workspace_key: str = Field(alias="workspaceKey")
    model_config = {"populate_by_name": True}
