from __future__ import annotations

from textual import on
from textual.css.query import NoMatches
from textual.widgets import Button

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.modals.create_issue_modal import CreateIssueModal


class IssueActionsMixin(ProjectHomeBase):
    """Create button behavior for the current [1] view."""

    @on(Button.Pressed, "#hub-new-issue")
    def _on_create_pressed(self) -> None:
        self.action_create()

    def action_create(self) -> None:
        if self._ui.view_mode == "sprints":
            self._open_create_sprint()
        elif self._ui.view_mode == "members":
            self._open_add_member()
        else:
            self._open_create_issue()

    def _open_add_member(self) -> None:
        from tissue.screens.project_home.modals.member_add_modal import MemberAddModal

        self.app.push_screen(
            MemberAddModal(project_key=self._project_key), self._on_members_added
        )

    def _on_members_added(self, added: bool | None) -> None:
        if added:
            self.run_worker(
                self._load_members_list(self._search_keyword()),
                exclusive=True,
                group="hub-list",
            )

    def _open_create_issue(self) -> None:
        self.app.push_screen(
            CreateIssueModal(
                project_key=self._project_key, members=self._member_list.members
            ),
            self._on_issue_created,
        )

    def _is_project_manager(self) -> bool:
        client = self.app.client
        profile = client.account.cached_profile if client is not None else None
        username = profile.username if profile is not None else None
        if not username:
            return False
        for member in self._member_list.members:
            if member.username == username:
                return (member.role or "").upper() == "MANAGER"
        return False

    def _update_create_button(self) -> None:
        try:
            create_button = self.query_one("#hub-new-issue", Button)
        except NoMatches:
            return
        mode = self._ui.view_mode
        if mode == "sprints":
            create_button.label = "+"
            manager = self._is_project_manager()
            create_button.disabled = not manager
            create_button.tooltip = "New sprint" if manager else "Requires manager role"
        elif mode == "members":
            create_button.label = "+"
            manager = self._is_project_manager()
            create_button.disabled = not manager
            create_button.tooltip = "Add member" if manager else "Requires manager role"
        else:
            create_button.label = "+"
            create_button.disabled = False
            create_button.tooltip = "New issue"

    def _on_issue_created(self, issue_key: str | None) -> None:
        if not issue_key:
            return
        self._set_view_chrome("issues")
        self.run_worker(
            self._reload_and_select(issue_key), exclusive=True, group="hub-list"
        )
        self.run_worker(self._load_header_stats(), exclusive=True, group="hub-header")

    async def _reload_and_select(self, issue_key: str) -> None:
        await self._load_issues()
        for index, issue in enumerate(self._issue_list.issues):
            if issue.issue_key == issue_key:
                self._select_issue(index)
                return
