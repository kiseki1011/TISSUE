import sys
from pathlib import Path

# Add the project root directory to sys.path to allow absolute imports (e.g., 'from src...')
# This fixes "ModuleNotFoundError: No module named 'src'"
root_path = Path(__file__).parent.parent
sys.path.append(str(root_path))

from textual.app import App, ComposeResult
from src.config import ConfigManager
from src.screens.connect_screen import ConnectScreen

class IssueManageApp(App):
    CSS = """
    #dialog {
        padding: 2;
        border: solid green;
        width: 60%;
        height: auto;
        align: center middle; 
        margin: 2;
    }
    .title {
        text-align: center;
        text-style: bold;
        margin-bottom: 1;
    }
    .subtitle {
        margin-top: 2;
        margin-bottom: 1;
        color: yellow;
    }
    """
    
    def __init__(self):
        super().__init__()
        self.config_manager = ConfigManager()

    def on_mount(self) -> None:
        # 앱 시작 시 실행
        config = self.config_manager.get_config()
        
        # 현재 설정된 서버가 없으면 연결 화면 띄우기
        if not config.current_server:
            self.push_screen(ConnectScreen(self.config_manager))
        else:
            # 설정된 서버가 있으면 (나중에 로그인 화면으로 이동)
            self.notify(f"Loaded server: {config.current_server}")
            self.push_screen(ConnectScreen(self.config_manager))

if __name__ == '__main__':
    app = IssueManageApp()
    app.run()