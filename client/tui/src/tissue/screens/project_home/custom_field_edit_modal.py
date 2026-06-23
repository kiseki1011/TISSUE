from __future__ import annotations

import logging
from decimal import Decimal, InvalidOperation
from typing import TYPE_CHECKING, Any

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import (
    Button,
    Input,
    ProgressBar,
    Select,
    SelectionList,
    Static,
    Switch,
    TextArea,
)
from textual_timepiece.pickers import DateOverlay, DatePicker
from whenever import Date, Instant, PlainDateTime

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.screens.project_home.issue_field_edit_modal import _DueDateTimePicker

if TYPE_CHECKING:
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail

log = logging.getLogger(__name__)

# Per-type dialog modifier class, so the CSS sizes each editor to its content
# (a tall TextArea, a wide calendar, a checklist that scrolls, etc.).
_FTYPE_CLASS = {
    "TEXT": "-text",
    "SHORT_TEXT": "-shorttext",
    "DATE": "-date",
    "TIMESTAMP": "-datetime",
    "PERCENTAGE": "-pct",
    "SELECT_OPTION": "-select",
    "CHECKLIST": "-checklist",
    "BOOLEAN": "-bool",
}
_PICKER_TYPES = {"DATE", "TIMESTAMP"}


class _FieldDatePicker(DatePicker):
    """A date-only picker whose overlay Escape actually collapses it — the same
    textual-timepiece workaround `_DueDateTimePicker` applies to the datetime
    picker (the `DateOverlay.Closed` handler is misnamed upstream, so it never
    fires while the calendar grid holds focus)."""

    @on(DateOverlay.Closed)
    def _on_overlay_closed(self, message: DateOverlay.Closed) -> None:
        message.stop()
        self.expanded = False
        toggle = self.query("#toggle-button")
        if toggle:
            toggle.first().focus()


class CustomFieldEditModal(TissueModal[bool | None]):
    """Edit one custom field value with a widget matched to its type:

    TEXT=TextArea, INTEGER/DECIMAL=Input, PERCENTAGE=Input+live ProgressBar,
    DATE=date picker, TIMESTAMP=datetime picker (local -> UTC instant),
    BOOLEAN=Switch, SELECT_OPTION=Select, CHECKLIST=SelectionList. Saves the typed
    value via `IssueService.update_custom_fields`, then dismisses `True` so the
    caller re-renders, or `None` on cancel.
    """

    CSS_PATH = "custom_field_edit_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(
        self,
        *,
        issue_key: str,
        field: CustomFieldValueInfo,
        options: list[FieldOptionDetail],
    ) -> None:
        super().__init__()
        self._issue_key = issue_key
        self._field_id = field.field_id
        self._ftype = field.issue_field_type or "TEXT"
        # Capitalise the first letter (the server stores lower/camelCase labels like
        # "version"), matching how the detail pane renders the field label.
        label = field.field_label or "Field"
        self._label = label[:1].upper() + label[1:]
        self._required = bool(field.required)
        self._value: Any = field.value
        self._options = options

    def compose(self) -> ComposeResult:
        dialog_classes = f"dialog {_FTYPE_CLASS.get(self._ftype, '')}".strip()
        with Container(id="cfe-dialog", classes=dialog_classes):
            yield from self._editor()
            yield Static("", id="cfe-status", classes="status-msg")
            with Horizontal(classes="cfe-actions"):
                yield Button("Cancel", id="cfe-cancel", classes="-btn-error")
                yield Button("Save", id="cfe-save", classes="-btn-success")

    def _editor(self) -> ComposeResult:
        ftype = self._ftype
        if ftype == "TEXT":
            yield TextArea(str(self._value or ""), id="cfe-text")
        elif ftype in ("INTEGER", "DECIMAL"):
            yield Input(
                value="" if self._value is None else str(self._value),
                type="integer" if ftype == "INTEGER" else "number",
                id="cfe-input",
            )
        elif ftype == "PERCENTAGE":
            initial = self._value if isinstance(self._value, (int, float)) else 0
            yield ProgressBar(total=100, show_eta=False, id="cfe-bar")
            yield Input(
                value="" if self._value is None else str(int(initial)),
                type="integer",
                placeholder="0-100",
                id="cfe-input",
            )
        elif ftype == "DATE":
            yield _FieldDatePicker(date=self._initial_date(), id="cfe-date")
        elif ftype == "TIMESTAMP":
            yield _DueDateTimePicker(value=self._initial_datetime(), id="cfe-datetime")
        elif ftype == "BOOLEAN":
            yield Switch(value=bool(self._value), id="cfe-switch")
        elif ftype == "SELECT_OPTION":
            choices = [(o.name or "-", o.id) for o in self._options if o.id is not None]
            # Only pre-select the stored id if it's still a valid option; otherwise
            # start blank (a stale/missing id would crash Select on mount).
            valid = any(o.id == self._value for o in self._options)
            # Select.NULL is the no-selection sentinel; Select.BLANK resolves to
            # Widget.BLANK (== False), which is rejected as an illegal value.
            yield Select(
                choices,
                value=self._value if valid else Select.NULL,
                id="cfe-select",
            )
        elif ftype == "CHECKLIST":
            checked = self._checked_ids()
            yield SelectionList[int](
                *[
                    (o.name or "-", o.id, o.id in checked)
                    for o in self._options
                    if o.id is not None
                ],
                id="cfe-checklist",
            )
        else:  # unknown type — fall back to a plain text input
            yield Input(value=str(self._value or ""), id="cfe-input")

    def _checked_ids(self) -> set[int]:
        if isinstance(self._value, dict):
            return {int(k) for k, v in self._value.items() if v}
        if isinstance(self._value, list):
            return {int(v) for v in self._value}
        return set()

    def _initial_date(self) -> Date | None:
        if not self._value:
            return None
        try:
            return Date.parse_iso(str(self._value))
        except ValueError:
            return None

    def _initial_datetime(self) -> PlainDateTime | None:
        """The server stores a UTC instant; the user edits in local time."""
        if not self._value:
            return None
        try:
            return Instant.parse_iso(str(self._value)).to_system_tz().to_plain()
        except ValueError:
            return None

    def on_mount(self) -> None:
        dialog = self.query_one("#cfe-dialog", Container)
        dialog.border_title = f"Edit {self._label}"
        dialog.border_subtitle = "Esc to cancel"
        if self._ftype in _PICKER_TYPES:
            picker = self.query_one(
                "#cfe-date" if self._ftype == "DATE" else "#cfe-datetime"
            )
            self.watch(picker, "expanded", self._on_picker_expanded)
            self.call_after_refresh(self._open_picker)
        else:
            focusable = (
                self.query("#cfe-text")
                or self.query("#cfe-input")
                or self.query("#cfe-select")
                or self.query("#cfe-switch")
                or self.query("#cfe-checklist")
            )
            if focusable:
                focusable.first().focus()

    def _open_picker(self) -> None:
        if self._ftype == "DATE":
            self.query_one("#cfe-date", _FieldDatePicker).expanded = True
        else:
            self.query_one("#cfe-datetime", _DueDateTimePicker).expanded = True

    def _on_picker_expanded(self, expanded: bool) -> None:
        self.query_one("#cfe-dialog", Container).set_class(bool(expanded), "-expanded")

    @on(Input.Changed, "#cfe-input")
    def _on_input_changed(self, event: Input.Changed) -> None:
        # Live-reflect a PERCENTAGE input on its progress bar.
        if self._ftype != "PERCENTAGE":
            return
        try:
            pct = max(0, min(100, int(event.value)))
        except ValueError:
            pct = 0
        self.query_one("#cfe-bar", ProgressBar).update(progress=pct)

    @on(Button.Pressed, "#cfe-cancel")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Input.Submitted, "#cfe-input")
    def _on_submit(self) -> None:
        self._save()

    @on(Button.Pressed, "#cfe-save")
    def _on_save(self) -> None:
        self._save()

    def _save(self) -> None:
        self.run_worker(self._do_save(), exclusive=True, group="cfe-save")

    def _error(self, message: str) -> None:
        self.query_one("#cfe-status", Static).update(message)

    def _payload_value(self) -> Any:
        """The typed value for the update payload, shaped per the field type.
        Raises ValueError with a user-facing message on bad input."""
        ftype = self._ftype
        if ftype == "TEXT":
            text = self.query_one("#cfe-text", TextArea).text
            if self._required and not text.strip():
                raise ValueError("This field is required.")
            return text
        if ftype == "BOOLEAN":
            return self.query_one("#cfe-switch", Switch).value
        if ftype == "INTEGER":
            raw = self.query_one("#cfe-input", Input).value.strip()
            try:
                return int(raw)
            except ValueError as e:
                raise ValueError("Must be a whole number.") from e
        if ftype == "DECIMAL":
            raw = self.query_one("#cfe-input", Input).value.strip()
            # Validate as an exact decimal (not float, which accepts nan/inf that
            # the server's BigDecimal rejects), then send the raw string so the
            # server keeps full precision.
            try:
                dec = Decimal(raw)
            except (InvalidOperation, ValueError) as e:
                raise ValueError("Must be a number.") from e
            if not dec.is_finite():
                raise ValueError("Must be a finite number.")
            return raw
        if ftype == "PERCENTAGE":
            raw = self.query_one("#cfe-input", Input).value.strip()
            try:
                pct = int(raw)
            except ValueError as e:
                raise ValueError("Must be a whole number 0-100.") from e
            if not (0 <= pct <= 100):
                raise ValueError("Must be between 0 and 100.")
            return pct
        if ftype == "DATE":
            picked = self.query_one("#cfe-date", _FieldDatePicker).date
            if picked is None:
                raise ValueError("Pick a date.")
            return picked.format_iso()
        if ftype == "TIMESTAMP":
            picked = self.query_one("#cfe-datetime", _DueDateTimePicker).datetime
            if picked is None:
                raise ValueError("Pick a date and time.")
            return picked.assume_system_tz().to_instant().format_iso()
        if ftype == "SELECT_OPTION":
            value = self.query_one("#cfe-select", Select).value
            if value is Select.NULL or value is Select.BLANK:
                raise ValueError("Pick an option.")
            return value
        if ftype == "CHECKLIST":
            selected = set(self.query_one("#cfe-checklist", SelectionList).selected)
            return {
                str(o.id): (o.id in selected) for o in self._options if o.id is not None
            }
        value = self.query_one("#cfe-input", Input).value.strip()
        if self._required and not value:
            raise ValueError("This field is required.")
        return value

    async def _do_save(self) -> None:
        client = self.app.client
        if client is None or self._field_id is None:
            return
        try:
            value = self._payload_value()
        except ValueError as e:
            self._error(str(e))
            return
        try:
            await client.issues.update_custom_fields(
                self._issue_key, {str(self._field_id): value}
            )
        except TissueApiError as e:
            self._error(getattr(e, "detail", None) or str(e) or "Update failed.")
            return
        self.dismiss(True)
