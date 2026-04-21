from tissue.config.manager import ConfigManager
from tissue.models.config import ServerHistoryItem


def get_server_history(config_manager: ConfigManager) -> list[ServerHistoryItem]:
    if config_manager.get_config().stub_mode:
        from tissue.dev.fixtures import STUB_SERVER_HISTORY

        return STUB_SERVER_HISTORY
    return config_manager.get_config().server_history
