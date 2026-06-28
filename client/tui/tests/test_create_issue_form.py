from __future__ import annotations

import unittest
from types import SimpleNamespace

from tissue.screens.project_home.modals.create_issue_form import (
    CreateIssueFormValues,
    member_choices,
    parent_candidate_labels,
    parent_hierarchy_of,
)


class CreateIssueFormValuesTest(unittest.TestCase):
    def test_standard_story_point_is_converted(self) -> None:
        values = _values(story_point_text="3", story_points_enabled=True)

        kwargs = values.to_create_kwargs()

        self.assertEqual(kwargs["story_point"], 3)

    def test_disabled_story_point_is_ignored(self) -> None:
        values = _values(story_point_text="not-a-number", story_points_enabled=False)

        kwargs = values.to_create_kwargs()

        self.assertIsNone(kwargs["story_point"])

    def test_enabled_story_point_must_be_numeric(self) -> None:
        values = _values(story_point_text="not-a-number", story_points_enabled=True)

        with self.assertRaisesRegex(ValueError, "Story points"):
            values.to_create_kwargs()

    def test_parent_is_required_for_subtask(self) -> None:
        values = _values(hierarchy="SUBTASK", parent_issue_key=None)

        with self.assertRaisesRegex(ValueError, "parent issue"):
            values.to_create_kwargs()

    def test_empty_custom_fields_are_not_sent(self) -> None:
        values = _values(custom_fields={})

        kwargs = values.to_create_kwargs()

        self.assertIsNone(kwargs["custom_fields"])


class CreateIssueHierarchyTest(unittest.TestCase):
    def test_parent_hierarchy_is_one_level_up(self) -> None:
        issue_types = [
            SimpleNamespace(id=1, hierarchy="EPIC"),
            SimpleNamespace(id=2, hierarchy="STANDARD"),
            SimpleNamespace(id=3, hierarchy="SUBTASK"),
        ]

        self.assertEqual(parent_hierarchy_of(issue_types, 3), "STANDARD")
        self.assertEqual(parent_hierarchy_of(issue_types, 2), "EPIC")
        self.assertIsNone(parent_hierarchy_of(issue_types, 1))

    def test_parent_candidates_match_required_hierarchy(self) -> None:
        issue_types = [
            SimpleNamespace(id=1, hierarchy="EPIC"),
            SimpleNamespace(id=2, hierarchy="STANDARD"),
        ]
        summaries = [
            SimpleNamespace(issue_key="TIS-1", issue_type_id=1, title="Epic"),
            SimpleNamespace(issue_key="TIS-2", issue_type_id=2, title="Task"),
            SimpleNamespace(issue_key=None, issue_type_id=1, title="No key"),
        ]

        candidates, labels = parent_candidate_labels(issue_types, "EPIC", summaries)

        self.assertEqual(candidates, [("TIS-1  Epic", "TIS-1")])
        self.assertEqual(labels, {"TIS-1": "TIS-1  Epic"})

    def test_member_choices_skip_members_without_id(self) -> None:
        members = [
            SimpleNamespace(member_id=1, display_name="Seungki", username="sk6449"),
            SimpleNamespace(member_id=None, display_name="No id", username="none"),
        ]

        self.assertEqual(member_choices(members), [("Seungki (@sk6449)", 1)])


def _values(
    *,
    issue_type_id: int | None = 1,
    hierarchy: str | None = "STANDARD",
    title: str = "Fix login",
    priority: str = "P2",
    assignee_member_id: int | None = None,
    story_point_text: str = "",
    story_points_enabled: bool = True,
    due_at: str | None = None,
    summary: str | None = None,
    content: str | None = None,
    custom_fields: dict[str, object] | None = None,
    parent_issue_key: str | None = None,
) -> CreateIssueFormValues:
    return CreateIssueFormValues(
        issue_type_id=issue_type_id,
        hierarchy=hierarchy,
        title=title,
        priority=priority,
        assignee_member_id=assignee_member_id,
        story_point_text=story_point_text,
        story_points_enabled=story_points_enabled,
        due_at=due_at,
        summary=summary,
        content=content,
        custom_fields={} if custom_fields is None else custom_fields,
        parent_issue_key=parent_issue_key,
    )
