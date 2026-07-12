import logging
import sys
import threading
from logging.handlers import RotatingFileHandler

from tissue.paths import state_dir

_crash_log = logging.getLogger("tissue.crash")


def _log_uncaught(exc_type, exc_value, exc_tb) -> None:
    """Log an exception that escaped Textual to the interpreter.

    Textual's handler covers message handlers and workers. An error in the run
    machinery itself would otherwise only print to the terminal.
    """
    _crash_log.critical(
        "Uncaught top-level exception", exc_info=(exc_type, exc_value, exc_tb)
    )


def _log_thread_exc(args) -> None:
    """Log an exception in a background thread (e.g. the input reader)."""
    _crash_log.critical(
        "Uncaught exception in thread %s",
        args.thread.name if args.thread else "?",
        exc_info=(args.exc_type, args.exc_value, args.exc_traceback),
    )


def setup_logging(*, debug: bool = False) -> None:
    log_dir = state_dir()
    log_dir.mkdir(parents=True, exist_ok=True)

    handler = RotatingFileHandler(
        log_dir / "tui.log", maxBytes=1_000_000, backupCount=3
    )
    handler.setFormatter(
        logging.Formatter("%(asctime)s [%(levelname)s] %(name)s: %(message)s")
    )

    root = logging.getLogger()
    root.setLevel(logging.DEBUG if debug else logging.INFO)
    root.addHandler(handler)

    # A crash under the alt-screen shows nothing on the terminal, so log any
    # route that bypasses Textual's own handler to `tui.log`.
    sys.excepthook = _log_uncaught
    threading.excepthook = _log_thread_exc

    if not debug:
        # httpx logs every request at INFO, which is mostly polling noise.
        logging.getLogger("httpx").setLevel(logging.WARNING)
