from textual.app import App
from textual.theme import Theme

CUSTOM_THEMES: list[Theme] = []


def register_custom_themes(app: App) -> None:
    for theme in CUSTOM_THEMES:
        app.register_theme(theme)
