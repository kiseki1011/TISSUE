from typing import TYPE_CHECKING

from textual.screen import Screen

if TYPE_CHECKING:
    from tissue.app import TissueApp


class TissueScreen(Screen):
    if TYPE_CHECKING:
        app: TissueApp
