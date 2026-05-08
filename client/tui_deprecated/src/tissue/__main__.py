from tissue.app import TissueApp
from tissue.logging_config import setup_logging


def main():
    setup_logging()
    app = TissueApp()
    app.run()


if __name__ == "__main__":
    main()
