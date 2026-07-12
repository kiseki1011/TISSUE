from __future__ import annotations

import unittest

from tissue.api.services.issues import _common_field_patch_body


class CommonFieldPatchBodyTest(unittest.TestCase):
    def test_empty_body_when_nothing_changes(self) -> None:
        body = _common_field_patch_body(
            title=None,
            summary=None,
            content=None,
            priority=None,
            due_at=None,
            clear_due_at=False,
        )

        self.assertEqual(body, {})

    def test_includes_only_provided_fields(self) -> None:
        body = _common_field_patch_body(
            title="New title",
            summary=None,
            content="Body",
            priority="P1",
            due_at="2026-06-28T00:00:00Z",
            clear_due_at=False,
        )

        self.assertEqual(
            body,
            {
                "title": "New title",
                "content": "Body",
                "priority": "P1",
                "dueAt": "2026-06-28T00:00:00Z",
            },
        )

    def test_clear_due_at_sends_explicit_null(self) -> None:
        body = _common_field_patch_body(
            title=None,
            summary=None,
            content=None,
            priority=None,
            due_at="2026-06-28T00:00:00Z",
            clear_due_at=True,
        )

        self.assertEqual(body, {"dueAt": None})


class RawPatchHooksTest(unittest.TestCase):
    """Guards the generated internals the raw PATCH workaround relies on."""

    def test_serialize_hooks_exist(self) -> None:
        from tissue.api.generated.api.issue_api import IssueApi
        from tissue.api.generated.api.sprint_api import SprintApi

        self.assertTrue(hasattr(IssueApi, "_update_issue_common_fields_serialize"))
        self.assertTrue(hasattr(SprintApi, "_update_sprint_serialize"))


class CommonFieldEditsTest(unittest.TestCase):
    ORIGINAL = {
        "title": "Old",
        "priority": "P2",
        "content": "Body",
        "dueAt": "2026-06-28T00:00:00Z",
    }

    def test_no_changes_is_empty(self) -> None:
        from tissue.screens.project_home.modals.edit_issue_modal import (
            common_field_edits,
        )

        edits = common_field_edits(
            title="Old",
            priority="P2",
            content="Body",
            due_at="2026-06-28T00:00:00Z",
            original=self.ORIGINAL,
        )

        self.assertEqual(edits, {})

    def test_only_changed_fields(self) -> None:
        from tissue.screens.project_home.modals.edit_issue_modal import (
            common_field_edits,
        )

        edits = common_field_edits(
            title="New",
            priority="P1",
            content="Body",
            due_at="2026-06-28T00:00:00Z",
            original=self.ORIGINAL,
        )

        self.assertEqual(edits, {"title": "New", "priority": "P1"})

    def test_clearing_due_sends_flag(self) -> None:
        from tissue.screens.project_home.modals.edit_issue_modal import (
            common_field_edits,
        )

        edits = common_field_edits(
            title="Old",
            priority="P2",
            content="Body",
            due_at="",
            original=self.ORIGINAL,
        )

        self.assertEqual(edits, {"clear_due_at": True})

    def test_setting_due_sends_value(self) -> None:
        from tissue.screens.project_home.modals.edit_issue_modal import (
            common_field_edits,
        )

        edits = common_field_edits(
            title="Old",
            priority="P2",
            content="Body",
            due_at="2026-07-01T00:00:00Z",
            original=self.ORIGINAL,
        )

        self.assertEqual(edits, {"due_at": "2026-07-01T00:00:00Z"})


class SprintFieldEditsTest(unittest.TestCase):
    ORIGINAL = {"title": "Sprint 1", "goal": "Ship it", "dueAt": "2026-07-01T00:00:00Z"}

    def test_no_changes_is_empty(self) -> None:
        from tissue.screens.project_home.modals.edit_sprint_modal import (
            sprint_field_edits,
        )

        edits = sprint_field_edits(
            title="Sprint 1",
            goal="Ship it",
            due_at="2026-07-01T00:00:00Z",
            original=self.ORIGINAL,
            show_due=True,
        )

        self.assertEqual(edits, {})

    def test_title_and_goal_changes(self) -> None:
        from tissue.screens.project_home.modals.edit_sprint_modal import (
            sprint_field_edits,
        )

        edits = sprint_field_edits(
            title="Sprint 2",
            goal="",
            due_at="2026-07-01T00:00:00Z",
            original=self.ORIGINAL,
            show_due=True,
        )

        self.assertEqual(edits, {"title": "Sprint 2", "goal": ""})

    def test_due_ignored_when_not_shown(self) -> None:
        from tissue.screens.project_home.modals.edit_sprint_modal import (
            sprint_field_edits,
        )

        edits = sprint_field_edits(
            title="Sprint 1",
            goal="Ship it",
            due_at="",
            original=self.ORIGINAL,
            show_due=False,
        )

        self.assertEqual(edits, {})
