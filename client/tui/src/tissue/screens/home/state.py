from __future__ import annotations

from dataclasses import dataclass, field
from typing import TYPE_CHECKING

from textual.timer import Timer

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.project_summary import ProjectSummary
    from tissue.api.generated.models.workflow_detail import WorkflowDetail


@dataclass
class DashboardProjectState:
    items: list[ProjectSummary] | None = None


@dataclass
class DashboardMyWorkState:
    items: list[IssueSummary] | None = None


@dataclass
class DashboardSearchState:
    kind: str | None = None
    results: list[ProjectSummary] | list[IssueSummary] | None = None
    keyword: str = ""
    generation: int = 0
    timer: Timer | None = None
    mounted_table_kind: str | None = None

    def invalidate(self) -> int:
        self.generation += 1
        return self.generation

    def clear_results(self) -> None:
        self.kind = None
        self.results = None
        self.keyword = ""
        self.mounted_table_kind = None


@dataclass
class DashboardWorkflowState:
    state_colors: dict[int, str] = field(default_factory=dict)
    workflow_cache: dict[int, WorkflowDetail] = field(default_factory=dict)
