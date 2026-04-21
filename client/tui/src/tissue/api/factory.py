from tissue.api.client import TissueClient
from tissue.config.manager import ConfigManager


def create_client(
    base_url: str, config_manager: ConfigManager | None = None
) -> TissueClient:
    if config_manager and config_manager.get_config().stub_mode:
        from tissue.dev.stub_client import StubTissueClient

        return StubTissueClient(base_url, config_manager)
    return TissueClient(base_url, config_manager)
