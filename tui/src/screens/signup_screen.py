from textual.app import ComposeResult
from textual.screen import Screen
from textual.widgets import Header, Footer, Input, Button, Label, Static
from textual.containers import Container, Horizontal
from textual import on, events
from src.api.client import SystemInfo, ServerClient
from src.config import ConfigManager
from src.i18n.manager import i18n
from src.assets.logo import TISSUE_LOGO

class SignupScreen(Screen):
    CSS = """
    #signup-container {
        height: auto;
        align: center middle;
    }

    #header_row {
        width: 100%;
        height: 1;
        align: left middle;
        margin-bottom: 0;
        padding-left: 1;
    }

    #back_btn {
        width: 3;
        height: 1;
        min-width: 3;
        padding: 0;
        margin: 0;
        border: none;
    }

    .title {
        margin-bottom: 2;
    }

    #email-row {
        height: 3;
        width: 100%;
        margin-bottom: 0;
    }
    
    #email {
        width: 78%; 
        height: 3;
        margin: 0;
    }
    
    #verify_btn {
        width: 20%;
        height: 3;
        margin-left: 1;
        min-width: 10;
    }
    
    .status-msg {
        height: 1;
        margin-bottom: 1;
        text-align: left;
        color: $text-muted;
        padding-left: 1;
        width: 100%;
    }

    .input-field {
        width: 100%;
        margin: 0;
    }

    #name {
        margin-top: 1;
    }

    #password {
        margin-top: 2; 
    }

    #btn-row {
        margin-top: 2;
        height: 3;
        width: 100%;
        align: center middle;
    }
    
    #submit_btn {
        width: 100%; 
        height: 3;
        margin: 0;
    }
    """

    def __init__(self, system_info: SystemInfo):
        super().__init__()
        self.system_info = system_info
        self.config_manager = ConfigManager()
        self.signup_token = None
        self.verification_id = None
        self.check_timer = None
        self.email_timer = None
        self.username_timer = None

    def on_mount(self) -> None:
        self.query_one("#email", Input).focus()
    
    # ... (compose remains same) ...

    # ... (handlers remain same) ...

    @on(Button.Pressed, "#verify_btn")
    async def on_verify(self):
        email = self.query_one("#email", Input).value
        if not email or "@" not in email:
            self.app.notify("Invalid email", severity="error")
            return
        client = ServerClient(self.config_manager.get_config().current_server)
        
        # Request verification and get secure ID
        ver_id = await client.request_verification(email)
        
        if ver_id:
            self.verification_id = ver_id
            self.app.notify("Email sent! Please check your inbox.", timeout=3)
            self.query_one("#verify_btn").disabled = True
            lbl = self.query_one("#email_status")
            lbl.update("Waiting for verification...")
            lbl.classes = "status-msg waiting"
            
            # Start secure polling
            self.check_timer = self.set_interval(2.0, lambda: self.check_status(client))
        else: 
            self.app.notify("Failed to send email", severity="error")

    async def check_status(self, client: ServerClient):
        if not self.verification_id:
            return

        signup_token = await client.get_verification_status(self.verification_id)
        
        if signup_token:
            self.signup_token = signup_token
            if self.check_timer: self.check_timer.stop()
            
            lbl = self.query_one("#email_status")
            lbl.update("✅ Email Verified!")
            lbl.classes = "status-msg success"
            
            self.query_one("#submit_btn").disabled = False
            self.query_one("#verify_btn").display = False

    @on(Button.Pressed, "#submit_btn")
    async def on_signup(self):
        vals = [self.query_one(f"#{i}", Input).value for i in ["email", "username", "name", "password"]]
        if not all(vals):
            self.app.notify(i18n.get("error_fill_all"), severity="error")
            return
            
        if not self.signup_token:
            self.app.notify("Email verification required", severity="error")
            return

        client = ServerClient(self.config_manager.get_config().current_server)
        # Pass the secure signup token
        if await client.signup(*vals, self.signup_token):
            self.app.notify(i18n.get("signup_success"), timeout=3)
            self.app.pop_screen()
        else: self.app.notify(i18n.get("signup_failed", reason="Conflict"), severity="error")

    @on(Button.Pressed, "#cancel_btn")
    def on_cancel(self):
        if self.check_timer: self.check_timer.stop()
        if self.email_timer: self.email_timer.stop()
        if self.username_timer: self.username_timer.stop()
        self.app.pop_screen()
