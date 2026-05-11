from textual.timer import Timer
from textual.widget import Widget


class Spinner:
    BRAILLE = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏"
    DOTS = "⣾⣽⣻⢿⡿⣟⣯⣷"
    STARS = "✶✸✹✺✹✷"
    DEFAULT_FRAMES = STARS
    DEFAULT_INTERVAL = 0.1

    def __init__(
        self,
        owner: Widget,
        target: Widget,
        frames: str = DEFAULT_FRAMES,
        interval: float = DEFAULT_INTERVAL,
    ):
        self._owner = owner
        self._target = target
        self._frames = frames
        self._interval = interval
        self._timer: Timer | None = None
        self._idx = 0
        self._text = ""

    def start(self, text: str = "") -> None:
        self._text = text
        self.stop()
        self._idx = 0
        self._tick()
        self._timer = self._owner.set_interval(self._interval, self._tick)

    def stop(self) -> None:
        if self._timer is not None:
            self._timer.stop()
            self._timer = None

    @property
    def is_running(self) -> bool:
        return self._timer is not None

    def _tick(self) -> None:
        char = self._frames[self._idx]
        self._idx = (self._idx + 1) % len(self._frames)
        rendered = f"{char} {self._text}" if self._text else char
        self._target.update(rendered)
