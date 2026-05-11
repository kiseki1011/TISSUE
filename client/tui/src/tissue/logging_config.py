import logging
from logging.handlers import RotatingFileHandler

from tissue.paths import state_dir


def setup_logging() -> None:
    log_dir = state_dir()
    log_dir.mkdir(parents=True, exist_ok=True)

    handler = RotatingFileHandler(
        log_dir / "tui.log", maxBytes=1_000_000, backupCount=3
    )
    handler.setFormatter(
        logging.Formatter("%(asctime)s [%(levelname)s] %(name)s: %(message)s")
    )
    logging.basicConfig(level=logging.INFO, handlers=[handler])
