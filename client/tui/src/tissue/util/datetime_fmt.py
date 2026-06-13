from datetime import datetime, timedelta


def format_relative(dt: datetime | None) -> str:
    """Format datetime.

    Format datetime as:
        - 'today at HH:MM'
        - 'yesterday at HH:MM',
        - 'YYYY-MM-DD HH:MM' otherwise
        - returns '-' for `None`
    """
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
