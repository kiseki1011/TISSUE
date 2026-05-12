from typing import TYPE_CHECKING, TypeVar

from textual.screen import ModalScreen, Screen

if TYPE_CHECKING:
    from tissue.app import TissueApp


class TissueScreen(Screen):
    if TYPE_CHECKING:
        app: TissueApp


_T = TypeVar("_T")


class TissueModal(ModalScreen[_T]):
    if TYPE_CHECKING:
        app: TissueApp
