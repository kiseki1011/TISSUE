from __future__ import annotations

import dataclasses


def filter_to_dict(filter_obj: object) -> dict:
    """Serialize a frozen filter dataclass to a JSON-friendly dict."""
    return dataclasses.asdict(filter_obj)  # type: ignore[call-overload]


def filter_from_dict[T](cls: type[T], data: dict) -> T:
    """Rebuild a filter dataclass, ignoring keys it no longer has.

    Tolerates filters saved by an older app version (extra/renamed fields) by
    keeping only the fields the class still declares.
    """
    fields = {field.name for field in dataclasses.fields(cls)}  # type: ignore[arg-type]
    return cls(**{key: value for key, value in data.items() if key in fields})
