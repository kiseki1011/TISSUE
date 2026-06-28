from pathlib import Path

from textual.widget import Widget
from textual.widgets import Label
from textual_image.renderable import Image as _AutoRenderable
from textual_image.renderable.tgp import Image as _TGPRenderable
from textual_image.widget import HalfcellImage as _HalfBlockImage
from textual_image.widget import TGPImage as _TGPImage

_TGP_AVAILABLE = _AutoRenderable is _TGPRenderable

# True when the terminal supports the Kitty terminal graphics protocol (TGP).
# When False, images fall back to half-block rendering.
TGP_AVAILABLE = _TGP_AVAILABLE

# TODO: render images when sixel is available


def make_icon_widget(image_path: Path) -> Widget:
    if not image_path.is_file():
        return Label("")
    if _TGP_AVAILABLE:
        return _TGPImage(str(image_path))
    return _HalfBlockImage(str(image_path))
