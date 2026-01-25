from textual.app import ComposeResult
from textual.screen import Screen
from textual.widgets import Header, Footer, Input, Button, Label
from textual.containers import Container
from textual import on

class LoginScreen(Screen):
    # CSS: 화면 스타일 정의 (Java의 CSS/JavaFX CSS와 유사)
    CSS = """
    #login-container {
        padding: 2;
        border: solid blue;
        width: 60%;
        height: auto;
        align: center middle; 
        margin: 2;
    }
    .input-field {
        margin-bottom: 1;
    }
    .title {
        text-align: center;
        text-style: bold;
        margin-bottom: 2;
    }
    """

    def compose(self) -> ComposeResult:
        """
        화면에 표시할 위젯들을 정의합니다.
        Java의 'add()' 메서드와 비슷하지만, 파이썬의 'yield'를 사용합니다.
        """
        yield Header()
        yield Container(
            Label("Log In", classes="title"),
            Input(placeholder="Username", id="username", classes="input-field"),
            Input(placeholder="Password", password=True, id="password", classes="input-field"),
            Button("Login", variant="primary", id="login_btn"),
            id="login-container"
        )
        yield Footer()

    @on(Button.Pressed, "#login_btn")
    def on_login(self):
        """로그인 버튼 클릭 시 실행될 이벤트 핸들러"""
        # self.query_one: ID로 위젯을 찾습니다. (JavaScript의 document.querySelector, JavaFX의 lookup과 유사)
        username = self.query_one("#username", Input).value
        password = self.query_one("#password", Input).value
        
        if username and password:
            self.app.notify(f"Welcome, {username}!")
            # TODO: 워크스페이스 목록 화면으로 이동
        else:
            self.app.notify("Please enter username and password.", severity="error")
