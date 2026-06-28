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
