from datetime import datetime, timedelta


def format_relative(dt: datetime | None) -> str:
    """Format a datetime, using friendly wording for today and yesterday."""
    if dt is None:
        return "-"
    now = datetime.now(tz=dt.tzinfo)
    today = now.date()
    yesterday = today - timedelta(days=1)
    time_str = dt.strftime("%H:%M")

    if dt.date() == today:
        return f"today at {time_str}"
    if dt.date() == yesterday:
        return f"yesterday at {time_str}"
    return dt.strftime("%Y-%m-%d %H:%M")


def format_date(dt: datetime | None) -> str:
    """Format the date only, for compact table columns."""
    if dt is None:
        return "-"
    return dt.strftime("%Y-%m-%d")
