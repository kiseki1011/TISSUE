from __future__ import annotations

from tissue.screens.project_home.areas.sprint_detail import SprintDetailMixin
from tissue.screens.project_home.areas.sprint_list import SprintListMixin


class SprintsMixin(SprintListMixin, SprintDetailMixin):
    """Sprint list and sprint detail behavior."""
