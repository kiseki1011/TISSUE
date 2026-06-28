from __future__ import annotations

from collections.abc import Iterable
from typing import TYPE_CHECKING

from textual.css.query import NoMatches
from textual.validation import ValidationResult
from textual.widgets import Input, Label

if TYPE_CHECKING:
    from textual.dom import DOMNode

_STATUS_CLASSES = ("-error", "-waiting", "-success")


def set_field_status(
    container: DOMNode, input_id: str, message: str = "", kind: str | None = None
) -> bool:
    """Update the status label paired with a form field."""
    return set_status_label(container, f"{input_id}_status", message, kind)


def set_status_label(
    container: DOMNode, label_id: str, message: str = "", kind: str | None = None
) -> bool:
    """Update a status label by id."""
    try:
        label = container.query_one(f"#{label_id}", Label)
    except NoMatches:
        return False

    label.remove_class(*_STATUS_CLASSES)
    label.update(message if kind is not None else "")
    if kind is not None:
        label.add_class(f"-{kind}")
    return True


def render_validation_status(
    container: DOMNode,
    input_id: str,
    value: str,
    result: ValidationResult | None,
) -> None:
    """Render a Textual validation result under its field."""
    if not value or result is None or result.is_valid:
        set_field_status(container, input_id)
        return

    failures = result.failure_descriptions
    set_field_status(container, input_id, failures[0] if failures else "", "error")


def first_empty_required_field(
    container: DOMNode,
    field_ids: Iterable[str],
    *,
    strip: bool = False,
    message: str = "Required field",
) -> Input | None:
    """Mark empty required inputs and return the first empty field."""
    first_empty: Input | None = None
    for field_id in field_ids:
        field_input = container.query_one(f"#{field_id}", Input)
        value = field_input.value.strip() if strip else field_input.value
        if value:
            continue
        set_field_status(container, field_id, message, "error")
        if first_empty is None:
            first_empty = field_input

    if first_empty is not None:
        first_empty.focus()
    return first_empty
