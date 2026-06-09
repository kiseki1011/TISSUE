from collections.abc import Iterable

# Color variants for buttons
# Covers every border style listed in `tissue.app.BORDER_STYLES`
_VARIANTS: dict[str, str] = {
    "secondary": "$secondary",
    "success": "$success",
    "warning": "$warning",
    "error": "$error",
}


def generate_btn_variant_css(border_styles: Iterable[str]) -> str:
    rules: list[str] = []
    for variant, color in _VARIANTS.items():
        selector = f".-btn-{variant}"
        rules.append(
            f"{selector} {{ "
            f"border: round {color}; "
            f"color: {color}; "
            f"border-title-color: {color}; "
            f"}}"
        )
        rules.append(
            f"{selector}:focus {{ "
            f"border: round $accent; "
            f"color: $accent; "
            f"border-title-color: $accent; "
            f"}}"
        )
        for style in border_styles:
            if style == "round":
                continue
            rules.append(
                f"App.-border-{style} {selector} {{ border: {style} {color}; }}"
            )
            rules.append(
                f"App.-border-{style} {selector}:focus {{ border: {style} $accent; }}"
            )
    return "\n".join(rules)
