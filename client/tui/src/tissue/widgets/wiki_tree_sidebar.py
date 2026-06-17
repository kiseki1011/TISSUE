from __future__ import annotations

from rich.text import Text
from textual.app import ComposeResult
from textual.containers import Container
from textual.widgets import LoadingIndicator, Static, Tree
from textual.widgets.tree import TreeNode

from tissue.api.generated.models.wiki_document_tree_node import WikiDocumentTreeNode


class WikiTreeSidebar(Container):
    """Left-docked directory tree for the Wiki tab. Toggled on/off like the
    profile sidebar.

    Built from the flat tree-node list (each node carries a parentDocumentId).
    Hierarchy is assembled here. Selecting a node bubbles a `Tree.NodeSelected` whose
    `node.data` is the wiki document id, handled by the parent screen.
    """

    DEFAULT_CLASSES = "panel"

    _MAX_TITLE_LEN = 20  # titles longer than this are truncated with an ellipsis

    DEFAULT_CSS = """
    WikiTreeSidebar {
        dock: left;
        width: 33;
        height: 1fr;
        margin-left: 1;
        overflow-y: auto;
        background: $surface;
        border-title-align: center;
    }
    WikiTreeSidebar #wiki-tree {
        width: 100%;
        height: auto;
    }
    WikiTreeSidebar .wiki-tree-msg {
        width: 100%;
        padding: 1;
        color: $text-muted;
    }
    """

    def __init__(
        self,
        nodes: list[WikiDocumentTreeNode] | None,
        *,
        error: str | None = None,
    ) -> None:
        super().__init__()
        # Avoid `_nodes`/`_error`
        self._doc_nodes = nodes
        self._error_text = error

    def compose(self) -> ComposeResult:
        if self._error_text is not None:
            yield Static(self._error_text, classes="wiki-tree-msg")
            return
        if self._doc_nodes is None:
            yield LoadingIndicator()
            return
        if not self._doc_nodes:
            yield Static("No documents yet.", classes="wiki-tree-msg")
            return
        tree: Tree[int] = Tree("Wiki", id="wiki-tree")
        tree.show_root = False
        tree.guide_depth = 3
        self._populate(tree)
        yield tree

    def on_mount(self) -> None:
        self.border_title = "Documents"

    @classmethod
    def _display_title(cls, raw: str | None) -> str:
        title = raw or "-"
        if len(title) > cls._MAX_TITLE_LEN:
            return title[: cls._MAX_TITLE_LEN] + "…"
        return title

    def _populate(self, tree: Tree[int]) -> None:
        nodes = self._doc_nodes or []
        ids = {n.id for n in nodes if n.id is not None}
        by_parent: dict[int | None, list[WikiDocumentTreeNode]] = {}
        for node in nodes:
            by_parent.setdefault(node.parent_document_id, []).append(node)

        visited: set[int] = set()

        def add_under(parent: TreeNode[int], parent_key: int | None) -> None:
            for node in by_parent.get(parent_key, []):
                if node.id is None or node.id in visited:
                    continue  # Guard against cyclic/duplicate parent pointers
                visited.add(node.id)
                title = self._display_title(node.title)
                if by_parent.get(node.id):
                    branch = parent.add(Text(title), data=node.id)
                    add_under(branch, node.id)
                else:
                    # Leaves have no ▶ toggle, so their titles would start two
                    # cells to the left of sibling branches. Pad to align.
                    parent.add_leaf(Text("  " + title), data=node.id)

        # Top level is the root documents (parent is None) and orphans whose parent ID
        # is not present in the list.
        top_keys = [k for k in by_parent if k is None or k not in ids]
        top_keys.sort(key=lambda k: (k is not None, k if k is not None else -1))
        for key in top_keys:
            add_under(tree.root, key)
