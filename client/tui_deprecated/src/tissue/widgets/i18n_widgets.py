from textual.containers import Container
from textual.widgets import Label, ListView

from tissue.i18n.manager import i18n
from tissue.widgets.bracket_button import BracketButton
from tissue.widgets.modal_input import ModalInput


class I18nLabel(Label):
    def __init__(self, key: str, **kwargs):
        fmt_args = kwargs.pop("fmt_args", {})
        super().__init__(i18n.get(key, **fmt_args), **kwargs)
        self._i18n_key = key
        self._i18n_fmt_args = fmt_args

    def on_mount(self) -> None:
        i18n.subscribe(self._refresh_i18n)

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh_i18n)

    def _refresh_i18n(self) -> None:
        self.update(i18n.get(self._i18n_key, **self._i18n_fmt_args))

    def set_i18n_key(self, key: str, **fmt_args) -> None:
        self._i18n_key = key
        self._i18n_fmt_args = fmt_args
        self._refresh_i18n()

    def clear_i18n(self) -> None:
        self._i18n_key = ""
        self._i18n_fmt_args = {}
        self.update("")


class I18nInput(ModalInput):
    def __init__(
        self,
        *,
        placeholder_key: str | None = None,
        title_key: str | None = None,
        **kwargs,
    ):
        kwargs.setdefault(
            "placeholder",
            i18n.get(placeholder_key) if placeholder_key else "",
        )
        super().__init__(**kwargs)
        self._placeholder_key = placeholder_key
        self._title_key = title_key

    def on_mount(self) -> None:
        self._refresh_i18n()
        i18n.subscribe(self._refresh_i18n)

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh_i18n)

    def _refresh_i18n(self) -> None:
        if self._placeholder_key:
            self.placeholder = i18n.get(self._placeholder_key)
        if self._title_key:
            self.border_title = i18n.get(self._title_key)


class I18nButton(BracketButton):
    def __init__(self, key: str, **kwargs):
        super().__init__(i18n.get(key), **kwargs)
        self._i18n_key = key

    def on_mount(self) -> None:
        i18n.subscribe(self._refresh_i18n)

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh_i18n)

    def _refresh_i18n(self) -> None:
        self.base_label = i18n.get(self._i18n_key)

    def set_i18n_key(self, key: str) -> None:
        self._i18n_key = key
        self._refresh_i18n()


class I18nContainer(Container):
    def __init__(
        self,
        *children,
        title_key: str | None = None,
        subtitle_key: str | None = None,
        **kwargs,
    ):
        super().__init__(*children, **kwargs)
        self._title_key = title_key
        self._subtitle_key = subtitle_key

    def on_mount(self) -> None:
        self._refresh_i18n()
        i18n.subscribe(self._refresh_i18n)

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh_i18n)

    def _refresh_i18n(self) -> None:
        if self._title_key:
            self.border_title = i18n.get(self._title_key)
        if self._subtitle_key:
            self.border_subtitle = i18n.get(self._subtitle_key)


class I18nListView(ListView):
    def __init__(self, *children, title_key: str | None = None, **kwargs):
        super().__init__(*children, **kwargs)
        self._title_key = title_key

    def on_mount(self) -> None:
        self._refresh_i18n()
        i18n.subscribe(self._refresh_i18n)

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh_i18n)

    def _refresh_i18n(self) -> None:
        if self._title_key:
            self.border_title = i18n.get(self._title_key)
