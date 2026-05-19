import argparse

from tissue.app import TissueApp
from tissue.logging_config import setup_logging


def main() -> None:
    parser = argparse.ArgumentParser(prog="tissue")
    parser.add_argument(
        "--debug",
        action="store_true",
        help="Enable DEBUG-level logging and notify popups for unhandled "
        "async-task exceptions (development only).",
    )
    args = parser.parse_args()

    setup_logging(debug=args.debug)
    TissueApp(debug=args.debug).run()


if __name__ == "__main__":
    main()
