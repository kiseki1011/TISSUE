from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Grid, Horizontal, Vertical, VerticalScroll
from textual.content import Content
from textual.coordinate import Coordinate
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Button, DataTable, Input, Rule, Select, Static

from tissue.api.errors import TissueApiError
from tissue.screens.base import RefreshableScreen
from tissue.screens.home.rendering import _fit, _truncate
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home.member_picker_modal import UNASSIGN, MemberPickerModal
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.color_type import chip_style, color_hex
from tissue.widgets.detail_row import detail_row
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.comment_detail_response import (
        CommentDetailResponse,
    )
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.issue_type_info import IssueTypeInfo
    from tissue.api.generated.models.project_member_info import ProjectMemberInfo
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )
    from tissue.api.generated.models.workflow_detail import WorkflowDetail

log = logging.getLogger(__name__)

# Priority has no server-defined colour, so the TUI fixes one: each level maps to
# a theme variable used as the chip *background* (P0 loudest, P4 softest).
_PRIORITY_VAR: dict[str, str] = {
    "P0": "error",
    "P1": "warning",
    "P2": "primary",
    "P3": "secondary",
    "P4": "success",
}


def _member_name(info: ProjectMemberInfo | None) -> str:
    if info is None:
        return "-"
    return info.display_name or info.username or "-"


class ProjectHomeScreen(RefreshableScreen):
    """Per-project hub: a master-detail view of the project's issues.

    Left holds the [1] Issues list; selecting a row renders that issue's read
    view in the focusable [2] Details pane on the right, where a top-right action
    row offers a workflow transition (dropdown + Go) and the assignee picker.
    """

    CSS_PATH = "project_home.tcss"

    BINDINGS = [
        Binding("1", "focus_issues", show=False),
        Binding("2", "focus_detail", show=False),
        # ctrl+digit also works while the search input has focus (a plain digit is
        # typed into the input there, never reaching the screen binding).
        Binding("ctrl+1", "focus_issues", show=False),
        Binding("ctrl+2", "focus_detail", show=False),
    ]

    def __init__(self, project_key: str, title: str | None = None) -> None:
        super().__init__()
        self._project_key = project_key
        self._title = title
        self._issues: list[IssueSummary] = []
        self._members: list[ProjectMemberSummary] = []
        self._detail_issue_key: str | None = None
        self._detail_assigned = False
        # Workflow graphs are cached by id; they barely change and several issues
        # share one — fetched only to label transitions with their target state.
        self._workflow_cache: dict[int, WorkflowDetail] = {}
        self._transitions_by_id: dict[int, AvailableTransition] = {}
        self._selected_transition_id: int | None = None
        # state-id -> #rrggbb, harvested from the project's workflows so the
        # issues table can tint each Status with its workflow-defined colour.
        self._state_colors: dict[int, str] = {}

    def top_bar_breadcrumb(self) -> str:
        return f"Projects ▸ {self._title or self._project_key}"

    def compose_content(self) -> ComposeResult:
        with Container(id="screen-body"):
            search = Input(placeholder="Search issues…", id="hub-search")
            search.border_title = "Search"
            yield search
            with Grid(id="hub-grid"):
                issues = Vertical(
                    Static("Loading…", classes="hub-muted"),
                    id="hub-issues-box",
                    classes="hub-box panel",
                )
                issues.border_title = "[1] Issues"
                yield issues
                detail = VerticalScroll(
                    Static("Select an issue to see details.", classes="hub-muted"),
                    id="hub-detail",
                )
                detail.border_title = "[2] Details"
                detail.can_focus = True
                yield detail

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.run_worker(self._load_issues(), exclusive=True, group="hub-issues")
        self.run_worker(self._load_members(), exclusive=True, group="hub-members")
        self.run_worker(self._load_state_colors(), exclusive=True, group="hub-colors")

    async def refresh_data(self) -> None:
        await self._load_issues()

    async def _load_members(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            self._members = await client.project_members.list_project_members(
                self._project_key
            )
        except TissueApiError as e:
            log.debug("Hub: failed to load members: %s", e)
            self._members = []

    async def _load_state_colors(self) -> None:
        """Build a state-id -> colour map from the project's workflows so the
        issues table can tint each Status with its workflow-defined colour.

        Best-effort: a failure just leaves table statuses uncoloured (the detail
        pane colours its Status straight from the issue's own state, so it never
        depends on this map)."""
        client = self.app.client
        if client is None:
            return
        try:
            summaries = await client.workflows.list_workflows()
        except TissueApiError as e:
            log.debug("Hub: failed to list workflows: %s", e)
            return
        colors: dict[int, str] = {}
        for summary in summaries:
            workflow_id = summary.id
            if workflow_id is None:
                continue
            workflow = self._workflow_cache.get(workflow_id)
            if workflow is None:
                try:
                    workflow = await client.workflows.get_workflow(workflow_id)
                except TissueApiError as e:
                    log.debug("Hub: failed to load workflow %s: %s", workflow_id, e)
                    continue
                self._workflow_cache[workflow_id] = workflow
            for s in workflow.states or []:
                if s.id is not None and s.color:
                    hex_color = color_hex(s.color)
                    if hex_color:
                        colors[s.id] = hex_color
        self._state_colors = colors
        self._recolor_table_status()

    def _recolor_table_status(self) -> None:
        """Repaint each Status cell in place once colours are known. In-place
        (`update_cell_at`) rather than a rebuild, so it never fights a concurrent
        issues load for the table id, and leaves the cursor where it is.

        No-op when the table isn't mounted yet — that load will already read the
        now-populated colour map when it builds the rows."""
        try:
            table = self.query_one("#hub-issues-table", DataTable)
        except NoMatches:
            return
        for row, issue in enumerate(self._issues):
            state_id = issue.current_state_id
            if state_id is None:
                continue
            hex_color = self._state_colors.get(state_id)
            if hex_color:
                table.update_cell_at(
                    Coordinate(row, 2),
                    self._color_chip(issue.current_state_label or "-", hex_color),
                )

    async def _load_issues(self, keyword: str | None = None) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.issues.search_project_issues(
                self._project_key, keyword=keyword
            )
            self._issues = list(page.content or [])
        except TissueApiError as e:
            log.debug("Hub: failed to load issues: %s", e)
            self._issues = []
        await self._render_issues()
        # Seed the detail pane with the first issue so it isn't blank on open.
        if self._issues:
            self._select_issue(0)

    async def _render_issues(self) -> None:
        box = self.query_one("#hub-issues-box")
        # The table has a fixed id, so the old one must be gone before the new
        # one mounts (else DuplicateIds) — await the removal.
        await box.remove_children()
        if not self._issues:
            await box.mount(Static("No issues.", classes="hub-muted"))
            return
        rows: list[list[str | Text]] = [
            [
                _fit(i.issue_key or "-", 11),
                Text(_truncate(i.title or "-", 20)),
                self._color_chip(
                    i.current_state_label or "-",
                    self._state_colors.get(i.current_state_id)
                    if i.current_state_id is not None
                    else None,
                ),
                self._priority_chip(i.priority),
            ]
            for i in self._issues
        ]
        await box.mount(
            _DashTable(
                [("Key", 11), ("Title", None), ("Status", 14), ("Priority", 8)],
                rows,
                id="hub-issues-table",
                classes="hub-table",
            )
        )

    @staticmethod
    def _color_chip(label: str, color: str | None) -> str | Text:
        """`label` as a solid pill — `color` fills the text *background* with a
        readable foreground. `color` is a ColorType enum name or an already-
        resolved hex; falls back to plain text when there's no colour."""
        style = chip_style(color)
        return Text(f" {label} ", style=style) if style else label

    def _priority_chip(self, priority: str | None) -> str | Text:
        """Pn as a background pill, coloured from a fixed priority->theme map."""
        if not priority:
            return "-"
        variable = _PRIORITY_VAR.get(priority)
        bg = self.app.theme_variables.get(variable) if variable else None
        return self._color_chip(priority, bg)

    @staticmethod
    def _type_text(issue_type: IssueTypeInfo | None) -> str | Text:
        """Issue type in bold (no colour)."""
        if issue_type is None:
            return "-"
        return Text(issue_type.display_name or "-", style="bold")

    @on(Input.Submitted, "#hub-search")
    def _on_search(self, event: Input.Submitted) -> None:
        keyword = event.value.strip() or None
        self.run_worker(self._load_issues(keyword), exclusive=True, group="hub-issues")

    @on(DataTable.RowHighlighted, "#hub-issues-table")
    def _on_issue_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_issue(event.cursor_row)

    @on(DataTable.RowSelected, "#hub-issues-table")
    def _on_issue_selected(self, event: DataTable.RowSelected) -> None:
        self._select_issue(event.cursor_row, focus_detail=True)

    def _select_issue(self, idx: int, *, focus_detail: bool = False) -> None:
        if not (0 <= idx < len(self._issues)):
            return
        issue_key = self._issues[idx].issue_key
        if issue_key is None:
            return
        self.run_worker(
            self._render_issue_detail(issue_key, focus_detail=focus_detail),
            exclusive=True,
            group="hub-detail",
        )

    async def _render_issue_detail(self, issue_key: str, *, focus_detail: bool) -> None:
        client = self.app.client
        if client is None:
            return
        self._detail_issue_key = issue_key
        try:
            issue = await client.issues.get_issue(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load issue %s: %s", issue_key, e)
            await self._mount_detail(
                [Static("Couldn't load issue.", classes="hub-muted")]
            )
            return
        self._detail_assigned = issue.assignee is not None
        try:
            transitions = await client.issues.get_transitions(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load transitions for %s: %s", issue_key, e)
            transitions = []
        target_labels = await self._target_state_labels(transitions)
        self._transitions_by_id = {
            t.transition_id: t for t in transitions if t.transition_id is not None
        }
        self._selected_transition_id = None
        await self._mount_detail(self._issue_widgets(issue, transitions, target_labels))
        self.run_worker(
            self._load_comments(issue_key), exclusive=True, group="hub-comments"
        )
        if focus_detail:
            self.query_one("#hub-detail").focus()

    async def _target_state_labels(
        self, transitions: list[AvailableTransition]
    ) -> dict[int, str]:
        """Map each transition id to its target state's label via the workflow
        graph (cached). The available-transitions response doesn't carry the
        target state, so it's looked up in the workflow."""
        client = self.app.client
        workflow_id = next(
            (t.workflow_id for t in transitions if t.workflow_id is not None), None
        )
        if client is None or workflow_id is None:
            return {}
        workflow = self._workflow_cache.get(workflow_id)
        if workflow is None:
            try:
                workflow = await client.workflows.get_workflow(workflow_id)
            except TissueApiError as e:
                log.debug("Hub: failed to load workflow %s: %s", workflow_id, e)
                return {}
            self._workflow_cache[workflow_id] = workflow
        state_label = {
            s.id: s.label for s in (workflow.states or []) if s.id is not None
        }
        return {
            wt.id: state_label.get(wt.target_state_id) or "?"
            for wt in (workflow.transitions or [])
            if wt.id is not None and wt.target_state_id is not None
        }

    def _issue_widgets(
        self,
        d: IssueCommonDetail,
        transitions: list[AvailableTransition],
        target_labels: dict[int, str],
    ) -> list[Widget]:
        state = d.current_state
        issue_type = d.issue_type
        current_state_label = (state.display_name if state else None) or "-"
        widgets: list[Widget] = [
            Static(d.title or "-", markup=False, classes="hub-detail-title"),
            self._action_row(transitions, current_state_label, target_labels),
        ]
        widgets.extend(
            [
                detail_row("Key", d.issue_key or "-"),
                detail_row(
                    "Status",
                    self._color_chip(
                        current_state_label, state.color if state else None
                    ),
                ),
                detail_row("Priority", self._priority_chip(d.priority)),
                detail_row("Type", self._type_text(issue_type)),
                detail_row("Assignee", _member_name(d.assignee)),
                detail_row("Author", _member_name(d.author)),
                detail_row(
                    "Story points",
                    "-" if d.story_point is None else str(d.story_point),
                ),
                detail_row("Due", format_relative(d.due_at)),
                detail_row("Created", format_relative(d.created_at)),
                detail_row("Updated", format_relative(d.last_updated_at)),
                Rule(),
            ]
        )
        content = (d.content or "").strip()
        widgets.append(
            Static(content, markup=False, classes="hub-content")
            if content
            else Static("No content.", classes="hub-muted")
        )
        widgets.extend(self._comment_section())
        return widgets

    def _comment_section(self) -> list[Widget]:
        """Comments header, the container they load into, and the add form. The
        container is populated asynchronously by `_load_comments` after mount."""
        return [
            Rule(),
            Static("Comments", classes="hub-section-title"),
            Vertical(
                Static("Loading…", classes="hub-muted"),
                id="hub-comments",
                classes="hub-comments",
            ),
            Horizontal(
                Input(
                    placeholder="Add a comment…",
                    id="hub-comment-input",
                    classes="hub-comment-input",
                ),
                Button(
                    "Comment", id="hub-comment-submit", classes="hub-comment-submit"
                ),
                classes="hub-comment-form",
            ),
        ]

    def _comment_widgets(self, c: CommentDetailResponse, depth: int) -> list[Widget]:
        """A comment as a bold meta line (author · when) + its body, recursing
        into nested replies (indented). Deleted bodies show a placeholder."""
        author = (c.author.display_name or c.author.username) if c.author else None
        meta = " · ".join([author or "?", format_relative(c.created_at)])
        if c.is_edited:
            meta += " (edited)"
        body = "[deleted]" if c.is_deleted else (c.content or "").strip()
        indent = " hub-comment-indent" if depth else ""
        out: list[Widget] = [
            Static(meta, markup=False, classes=f"hub-comment-meta{indent}"),
            Static(body, markup=False, classes=f"hub-comment-body{indent}"),
        ]
        for reply in c.replies or []:
            out.extend(self._comment_widgets(reply, depth + 1))
        return out

    def _action_row(
        self,
        transitions: list[AvailableTransition],
        current_state_label: str,
        target_labels: dict[int, str],
    ) -> Vertical:
        """Top-right controls: a transition dropdown + ▶ to run it, with the
        assignee text button beneath."""
        rows: list[Widget] = []
        options = [
            (
                self._transition_label(t, current_state_label, target_labels),
                t.transition_id,
            )
            for t in transitions
            if t.transition_id is not None
        ]
        if options:
            rows.append(
                Horizontal(
                    Select(
                        options,
                        prompt="Transition",
                        id="hub-transition-select",
                        classes="hub-transition-select",
                    ),
                    Button(
                        # Content (not a plain str) so the brackets aren't parsed
                        # as console markup and dropped.
                        Content("[T]"),
                        id="hub-transition-go",
                        classes="hub-go-btn",
                        disabled=True,
                    ),
                    classes="hub-transition-row",
                )
            )
        rows.append(
            Horizontal(
                TextButton(
                    "Assignee", id="hub-assignee-btn", classes="hub-assignee-btn"
                ),
                classes="hub-assignee-row",
            )
        )
        return Vertical(*rows, classes="hub-action-col")

    @staticmethod
    def _transition_label(
        t: AvailableTransition,
        current_state_label: str,
        target_labels: dict[int, str],
    ) -> str:
        target = target_labels.get(t.transition_id) if t.transition_id else None
        label = f"{t.display_label or '?'}: {current_state_label} → {target or '?'}"
        if not t.can_execute and t.blocked_reasons:
            reasons = [r.message for r in t.blocked_reasons if r.message]
            if reasons:
                label += f"  ⚠ {'; '.join(reasons)}"
        return label

    @on(Select.Changed, "#hub-transition-select")
    def _on_transition_selected(self, event: Select.Changed) -> None:
        try:
            go = self.query_one("#hub-transition-go", Button)
        except NoMatches:
            return
        value = event.value
        if not isinstance(value, int):  # Select.BLANK
            self._selected_transition_id = None
            go.disabled = True
            go.tooltip = None
            return
        self._selected_transition_id = value
        transition = self._transitions_by_id.get(value)
        go.disabled = not (transition and transition.can_execute)
        go.tooltip = None
        if transition and not transition.can_execute and transition.blocked_reasons:
            reasons = [r.message for r in transition.blocked_reasons if r.message]
            if reasons:
                go.tooltip = "; ".join(reasons)

    @on(Button.Pressed, "#hub-transition-go")
    def _on_transition_go(self) -> None:
        issue_key = self._detail_issue_key
        transition_id = self._selected_transition_id
        if issue_key is None or transition_id is None:
            return
        self.run_worker(
            self._perform_transition(issue_key, transition_id),
            exclusive=True,
            group="hub-detail",
        )

    async def _perform_transition(self, issue_key: str, transition_id: int) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.issues.perform_transition(issue_key, transition_id)
        except TissueApiError as e:
            log.debug("Hub: transition failed for %s: %s", issue_key, e)
            self.app.notify("Transition failed.", severity="error")
            return
        # Re-render the detail for the new state + newly available transitions.
        # The list row's Status is refreshed on the next `r`.
        await self._render_issue_detail(issue_key, focus_detail=False)

    @on(Button.Pressed, "#hub-assignee-btn")
    def _on_assignee_pressed(self) -> None:
        if self._detail_issue_key is None:
            return
        self.app.push_screen(
            MemberPickerModal(self._members, assigned=self._detail_assigned),
            self._on_member_picked,
        )

    def _on_member_picked(self, result: int | None) -> None:
        issue_key = self._detail_issue_key
        if result is None or issue_key is None:
            return
        worker = (
            self._unassign(issue_key)
            if result == UNASSIGN
            else self._assign(issue_key, result)
        )
        self.run_worker(worker, exclusive=True, group="hub-detail")

    async def _assign(self, issue_key: str, member_id: int) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.issues.assign_issue(issue_key, member_id)
        except TissueApiError as e:
            log.debug("Hub: assign failed for %s: %s", issue_key, e)
            self.app.notify("Assign failed.", severity="error")
            return
        await self._render_issue_detail(issue_key, focus_detail=False)

    async def _unassign(self, issue_key: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.issues.unassign_issue(issue_key)
        except TissueApiError as e:
            log.debug("Hub: unassign failed for %s: %s", issue_key, e)
            self.app.notify("Unassign failed.", severity="error")
            return
        await self._render_issue_detail(issue_key, focus_detail=False)

    async def _load_comments(self, issue_key: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            comments = await client.comments.list_issue_comments(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load comments for %s: %s", issue_key, e)
            comments = []
        # The detail may have been rebuilt for another issue while we awaited.
        if self._detail_issue_key != issue_key:
            return
        try:
            box = self.query_one("#hub-comments")
        except NoMatches:
            return
        await box.remove_children()
        if not comments:
            await box.mount(Static("No comments yet.", classes="hub-muted"))
            return
        widgets: list[Widget] = []
        for comment in comments:
            widgets.extend(self._comment_widgets(comment, depth=0))
        await box.mount(*widgets)

    @on(Input.Submitted, "#hub-comment-input")
    def _on_comment_input_submitted(self, event: Input.Submitted) -> None:
        self._submit_comment(event.value)

    @on(Button.Pressed, "#hub-comment-submit")
    def _on_comment_submit_pressed(self) -> None:
        try:
            value = self.query_one("#hub-comment-input", Input).value
        except NoMatches:
            return
        self._submit_comment(value)

    def _submit_comment(self, text: str) -> None:
        issue_key = self._detail_issue_key
        text = text.strip()
        if issue_key is None or not text:
            return
        self.run_worker(
            self._post_comment(issue_key, text),
            exclusive=True,
            group="hub-comment-post",
        )

    async def _post_comment(self, issue_key: str, text: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.comments.create_comment(issue_key, text)
        except TissueApiError as e:
            log.debug("Hub: failed to add comment to %s: %s", issue_key, e)
            self.app.notify("Failed to add comment.", severity="error")
            return
        if self._detail_issue_key != issue_key:
            return
        try:
            self.query_one("#hub-comment-input", Input).value = ""
        except NoMatches:
            pass
        await self._load_comments(issue_key)

    async def _mount_detail(self, widgets: list[Widget]) -> None:
        detail = self.query_one("#hub-detail")
        # The action-row controls carry fixed ids, so the old set must be gone
        # before the new mounts (else DuplicateIds) — await the removal.
        await detail.remove_children()
        await detail.mount(*widgets)

    def action_focus_issues(self) -> None:
        try:
            self.query_one("#hub-issues-table", DataTable).focus()
        except NoMatches:
            pass

    def action_focus_detail(self) -> None:
        self.query_one("#hub-detail").focus()
