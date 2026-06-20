from __future__ import annotations

from rich.style import Style
from rich.text import Text
from textual.widgets import Tree
from textual.widgets.tree import TreeNode

# Meta marking a label segment as a click target for expand/collapse.
_TOGGLE_META = Style.from_meta({"toggle": True})


class _WikiTree(Tree[int]):
    """Document tree that swaps the expand/collapse triangle for a book/page
    icon: a doc with sub-docs shows 📖, a leaf doc shows 📄.

    The book icon stays clickable to expand/collapse (the triangle's old job);
    clicking a label (or pressing Enter) opens the doc.
    """

    def render_label(
        self, node: TreeNode[int], base_style: Style, style: Style
    ) -> Text:
        label = node._label.copy()  # _label is the Text; .label getter is TextType
        label.stylize(style)
        if node.parent is None:  # the hidden root, never shown
            return label
        icon = "📖 " if node.children else "📄 "
        toggle = _TOGGLE_META if node.allow_expand else Style()
        return Text.assemble((icon, base_style + toggle), label)
