from typing import Protocol, runtime_checkable


@runtime_checkable
class Refreshable(Protocol):
    """Protocol for data re-fetch and re-render on demand.

    Implementation should:
      1. Fetch data from the server (or cache)
      2. Populate the visible widget(s)
      3. Log failures
    """

    async def refresh_data(self) -> None: ...
