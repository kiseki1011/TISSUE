from textual import events
from textual.widgets import Input


class ModalInput(Input):
    _editing: bool = False

    def on_focus(self, event: events.Focus) -> None:
        self._editing = False
        self.remove_class("editing")

    def check_consume_key(self, key: str, character: str | None) -> bool:
        if not self._editing:
            return False
        return super().check_consume_key(key, character)

    async def _on_key(self, event: events.Key) -> None:
        if self._editing and event.key == "escape":
            self._editing = False
            self.remove_class("editing")
            event.stop()
            return

        if not self._editing and event.is_printable:
            self._editing = True
            self.add_class("editing")

        await super()._on_key(event)

    async def action_submit(self) -> None:
        await super().action_submit()
