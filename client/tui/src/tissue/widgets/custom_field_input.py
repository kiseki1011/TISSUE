"""An inline, embeddable editor for a single custom field, with the control
chosen by the field's type. Unlike `CustomFieldEditModal` (which edits one field
in a dedicated dialog), this is a plain widget that any number of instances can
share a screen — the create-issue form mounts one per field. Queries are scoped
to the widget (class selectors, not global ids), so instances never collide.
"""

from __future__ import annotations

import logging
from decimal import Decimal, InvalidOperation
from typing import TYPE_CHECKING, Any

from textual import on
from textual.app import ComposeResult
from textual.containers import Vertical
from textual.css.query import NoMatches
from textual.widgets import (
    Input,
    ProgressBar,
    Select,
    SelectionList,
    Switch,
    TextArea,
)
from whenever import Date, Instant, PlainDateTime

from tissue.widgets.datetime_pickers import DueDateTimePicker, FieldDatePicker

if TYPE_CHECKING:
    from textual.widget import Widget

    from tissue.api.generated.models.field_option_detail import FieldOptionDetail

log = logging.getLogger(__name__)

_SHORT_TEXT_MAX = 50

# Returned by get_value() for an untouched optional field, so the caller omits it
# from the payload (vs a real value, or a ValueError for a required-blank field).
# A unique object so it can't collide with any legitimate field value.
UNSET: Any = object()


class CustomFieldInput(Vertical):
    """A type-matched control for one custom field (the field name sits in the
    control's border title).

    TEXT=TextArea, SHORT_TEXT/INTEGER/DECIMAL=Input, PERCENTAGE=Input+live
    ProgressBar, DATE/TIMESTAMP=picker (stored as ISO date / UTC instant),
    BOOLEAN=Switch, SELECT_OPTION=Select, CHECKLIST=SelectionList.

    `get_value()` reads and shapes the typed value (raising ValueError with a
    user-facing message on bad input); an untouched optional field yields `UNSET`
    so the caller can omit it from the create payload.
    """

    def __init__(
        self,
        *,
        field_id: int,
        label: str,
        ftype: str,
        required: bool,
        options: list[FieldOptionDetail],
        value: Any = None,
    ) -> None:
        super().__init__(classes="custom-field-input")
        self._field_id = field_id
        self._label = label
        self._ftype = ftype
        self._required = required
        self._options = options
        self._value = value

    @property
    def field_id(self) -> int:
        return self._field_id

    @property
    def ftype(self) -> str:
        return self._ftype

    def compose(self) -> ComposeResult:
        yield from self._control()

    def _title(self) -> str:
        return f"{self._label}{' *' if self._required else ''}"

    def _control(self) -> ComposeResult:
        """Yield the type-matched control(s), with the field name in the control's
        border title (so it reads as a labelled box, no separate label above)."""
        ftype = self._ftype
        title = self._title()
        if ftype == "TEXT":
            text = TextArea(str(self._value or ""), classes="cf-text")
            text.border_title = title
            yield text
        elif ftype == "SHORT_TEXT":
            inp = Input(
                value="" if self._value is None else str(self._value),
                max_length=_SHORT_TEXT_MAX,
                classes="cf-input",
            )
            inp.border_title = title
            yield inp
        elif ftype in ("INTEGER", "DECIMAL"):
            inp = Input(
                value="" if self._value is None else str(self._value),
                type="integer" if ftype == "INTEGER" else "number",
                classes="cf-input",
            )
            inp.border_title = title
            yield inp
        elif ftype == "PERCENTAGE":
            initial = self._value if isinstance(self._value, (int, float)) else None
            yield ProgressBar(total=100, show_eta=False, classes="cf-bar")
            inp = Input(
                value="" if initial is None else str(int(initial)),
                type="integer",
                placeholder="0-100",
                classes="cf-input",
            )
            inp.border_title = title
            yield inp
        elif ftype == "DATE":
            date = FieldDatePicker(date=self._initial_date(), classes="cf-date")
            date.border_title = title
            yield date
        elif ftype == "TIMESTAMP":
            dt = DueDateTimePicker(
                value=self._initial_datetime(), classes="cf-datetime"
            )
            dt.border_title = title
            yield dt
        elif ftype == "BOOLEAN":
            switch = Switch(value=bool(self._value), classes="cf-switch")
            switch.border_title = title
            yield switch
        elif ftype == "SELECT_OPTION":
            choices = [(o.name or "-", o.id) for o in self._options if o.id is not None]
            # Only pre-select a stored id if it's still a valid option; a stale id
            # would crash Select on mount. Select.NULL is the no-selection
            # sentinel (Select.BLANK resolves to Widget.BLANK == False, rejected).
            valid = any(o.id == self._value for o in self._options)
            select = Select(
                choices,
                value=self._value if valid else Select.NULL,
                classes="cf-select",
            )
            select.border_title = title
            yield select
        elif ftype == "CHECKLIST":
            checked = self._checked_ids()
            checklist = SelectionList[int](
                *[
                    (o.name or "-", o.id, o.id in checked)
                    for o in self._options
                    if o.id is not None
                ],
                classes="cf-checklist",
            )
            checklist.border_title = title
            yield checklist
        else:  # unknown type — fall back to a plain text input
            inp = Input(value=str(self._value or ""), classes="cf-input")
            inp.border_title = title
            yield inp

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

    @property
    def picker(self) -> Widget | None:
        """The date/datetime picker control, if this field uses one (so a host
        can auto-open it or watch its `expanded` state)."""
        selector = (
            ".cf-date"
            if self._ftype == "DATE"
            else ".cf-datetime"
            if self._ftype == "TIMESTAMP"
            else None
        )
        if selector is None:
            return None
        try:
            return self.query_one(selector)
        except NoMatches:
            return None

    def focus_input(self) -> None:
        """Move focus to this field's control (first one that exists)."""
        controls = (
            ".cf-text",
            ".cf-input",
            ".cf-select",
            ".cf-switch",
            ".cf-checklist",
        )
        for selector in controls:
            try:
                self.query_one(selector).focus()
                return
            except NoMatches:
                continue

    @on(Input.Changed)
    def _on_input_changed(self, event: Input.Changed) -> None:
        # Live-reflect a PERCENTAGE input on its progress bar. Scoped to this
        # widget — the event bubbles up from this instance's own Input.
        if self._ftype != "PERCENTAGE":
            return
        try:
            pct = max(0, min(100, int(event.value)))
        except ValueError:
            pct = 0
        self.query_one(".cf-bar", ProgressBar).update(progress=pct)

    def _required_error(self) -> ValueError:
        return ValueError(f"{self._label}: this field is required.")

    def get_value(self) -> Any:
        """The typed value for the create payload, shaped per the field type.

        Returns `UNSET` for an untouched optional field (so the caller omits it),
        a shaped value otherwise, and raises ValueError (with a user-facing
        message) on invalid input or a missing required field."""
        ftype = self._ftype
        if ftype == "TEXT":
            text = self.query_one(".cf-text", TextArea).text
            if not text.strip():
                if self._required:
                    raise self._required_error()
                return UNSET
            return text
        if ftype == "BOOLEAN":
            # A Switch always has a definite state; send it as-is.
            return self.query_one(".cf-switch", Switch).value
        if ftype == "DATE":
            picked = self.query_one(".cf-date", FieldDatePicker).date
            if picked is None:
                if self._required:
                    raise ValueError(f"{self._label}: pick a date.")
                return UNSET
            return picked.format_iso()
        if ftype == "TIMESTAMP":
            picked = self.query_one(".cf-datetime", DueDateTimePicker).datetime
            if picked is None:
                if self._required:
                    raise ValueError(f"{self._label}: pick a date and time.")
                return UNSET
            return picked.assume_system_tz().to_instant().format_iso()
        if ftype == "SELECT_OPTION":
            value = self.query_one(".cf-select", Select).value
            if value is Select.NULL or value is Select.BLANK:
                if self._required:
                    raise ValueError(f"{self._label}: pick an option.")
                return UNSET
            return value
        if ftype == "CHECKLIST":
            selected = set(self.query_one(".cf-checklist", SelectionList).selected)
            if not selected:
                if self._required:
                    raise ValueError(f"{self._label}: pick at least one option.")
                return UNSET
            return {
                str(o.id): (o.id in selected) for o in self._options if o.id is not None
            }
        # The remaining types share a single Input.
        raw = self.query_one(".cf-input", Input).value.strip()
        if not raw:
            if self._required:
                raise self._required_error()
            return UNSET
        if ftype == "INTEGER":
            try:
                return int(raw)
            except ValueError as e:
                raise ValueError(f"{self._label}: must be a whole number.") from e
        if ftype == "DECIMAL":
            # Validate as an exact decimal (not float, which accepts nan/inf the
            # server's BigDecimal rejects), then send the raw string for precision.
            try:
                dec = Decimal(raw)
            except (InvalidOperation, ValueError) as e:
                raise ValueError(f"{self._label}: must be a number.") from e
            if not dec.is_finite():
                raise ValueError(f"{self._label}: must be a finite number.")
            return raw
        if ftype == "PERCENTAGE":
            try:
                pct = int(raw)
            except ValueError as e:
                raise ValueError(f"{self._label}: must be a whole number 0-100.") from e
            if not (0 <= pct <= 100):
                raise ValueError(f"{self._label}: must be between 0 and 100.")
            return pct
        return raw
