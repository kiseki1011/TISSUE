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
# from the payload. A unique object so it can't collide with any real field value.
UNSET: Any = object()


class CustomFieldInput(Vertical):
    """A type-matched control for one custom field.

    The field name sits in the control's border title. A plain widget, so a
    screen can mount many at once. The create-issue form mounts one per field.
    Queries are scoped to the widget with class selectors, not global ids, so
    instances never collide.

    The control is chosen by field type:
        - TEXT = TextArea
        - SHORT_TEXT / INTEGER / DECIMAL = Input
        - PERCENTAGE = Input + live ProgressBar
        - DATE / TIMESTAMP = picker (stored as ISO date / UTC instant)
        - BOOLEAN = Switch
        - SELECT_OPTION = Select
        - CHECKLIST = SelectionList

    `get_value()` reads and shapes the typed value, raising ValueError with a
    user-facing message on bad input. An untouched optional field yields `UNSET`
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
        """Yield the type-matched control(s).

        The field name goes in the control's border title, so it reads as a
        labeled box with no separate label above.
        """
        ftype = self._ftype
        title = self._title()
        if ftype == "TEXT":
            text = TextArea(str(self._value or ""), classes="cf-text")
            text.border_title = title
            yield text
        elif ftype == "SHORT_TEXT":
            text_input = Input(
                value="" if self._value is None else str(self._value),
                max_length=_SHORT_TEXT_MAX,
                classes="cf-input",
            )
            text_input.border_title = title
            yield text_input
        elif ftype in ("INTEGER", "DECIMAL"):
            number_input = Input(
                value="" if self._value is None else str(self._value),
                type="integer" if ftype == "INTEGER" else "number",
                classes="cf-input",
            )
            number_input.border_title = title
            yield number_input
        elif ftype == "PERCENTAGE":
            initial = self._value if isinstance(self._value, (int, float)) else None
            yield ProgressBar(total=100, show_eta=False, classes="cf-bar")
            percent_input = Input(
                value="" if initial is None else str(int(initial)),
                type="integer",
                placeholder="0-100",
                classes="cf-input",
            )
            percent_input.border_title = title
            yield percent_input
        elif ftype == "DATE":
            date = FieldDatePicker(date=self._initial_date(), classes="cf-date")
            date.border_title = title
            yield date
        elif ftype == "TIMESTAMP":
            datetime_picker = DueDateTimePicker(
                value=self._initial_datetime(), classes="cf-datetime"
            )
            datetime_picker.border_title = title
            yield datetime_picker
        elif ftype == "BOOLEAN":
            switch = Switch(value=bool(self._value), classes="cf-switch")
            switch.border_title = title
            yield switch
        elif ftype == "SELECT_OPTION":
            choices = [
                (option.name or "-", option.id)
                for option in self._options
                if option.id is not None
            ]
            # Only pre-select a stored id if it's still a valid option, since a
            # stale id would crash Select on mount. Select.NULL is the no-selection
            # special value (Select.BLANK is Widget.BLANK == False, which Select
            # rejects).
            valid = any(option.id == self._value for option in self._options)
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
                    (option.name or "-", option.id, option.id in checked)
                    for option in self._options
                    if option.id is not None
                ],
                classes="cf-checklist",
            )
            checklist.border_title = title
            yield checklist
        else:  # unknown type, fall back to a plain text input
            fallback_input = Input(value=str(self._value or ""), classes="cf-input")
            fallback_input.border_title = title
            yield fallback_input

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
        """The server stores a UTC instant, but the user edits in local time."""
        if not self._value:
            return None
        try:
            return Instant.parse_iso(str(self._value)).to_system_tz().to_plain()
        except ValueError:
            return None

    @property
    def picker(self) -> Widget | None:
        """The date/datetime picker control, if this field uses one.

        Lets a host auto-open it or watch its `expanded` state.
        """
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
        # Live-reflect a PERCENTAGE input on its progress bar. The event bubbles
        # up from this instance's own Input, so it stays scoped to this widget.
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

        Returns `UNSET` for an untouched optional field so the caller omits it,
        a shaped value otherwise. Raises ValueError with a user-facing message
        on invalid input or a missing required field.
        """
        ftype = self._ftype
        if ftype == "TEXT":
            text = self.query_one(".cf-text", TextArea).text
            if not text.strip():
                if self._required:
                    raise self._required_error()
                return UNSET
            return text
        if ftype == "BOOLEAN":
            # A Switch always has a definite state, so send it as-is.
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
                str(option.id): (option.id in selected)
                for option in self._options
                if option.id is not None
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
            except ValueError as error:
                raise ValueError(f"{self._label}: must be a whole number.") from error
        if ftype == "DECIMAL":
            # Validate as an exact decimal, not float, since float accepts the
            # nan/inf the server's BigDecimal rejects. Send the raw string so
            # precision is preserved.
            try:
                dec = Decimal(raw)
            except (InvalidOperation, ValueError) as error:
                raise ValueError(f"{self._label}: must be a number.") from error
            if not dec.is_finite():
                raise ValueError(f"{self._label}: must be a finite number.")
            return raw
        if ftype == "PERCENTAGE":
            try:
                pct = int(raw)
            except ValueError as error:
                raise ValueError(
                    f"{self._label}: must be a whole number 0-100."
                ) from error
            if not (0 <= pct <= 100):
                raise ValueError(f"{self._label}: must be between 0 and 100.")
            return pct
        return raw
