from typing import Protocol, runtime_checkable


@runtime_checkable
class Refreshable(Protocol):
    """Protocol for data re-fetch and re-render on demand.

    An implementation should:
        - Fetch data from the server (or cache)
        - Populate the visible widget(s)
        - Log failures
    """

    async def refresh_data(self) -> None: ...
