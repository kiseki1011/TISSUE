from datetime import datetime, timedelta

from tissue.i18n.manager import i18n


def format_relative(dt: datetime | None) -> str:
    """Format datetime as: 'today at HH:MM', 'yesterday at HH:MM',
    or 'YYYY-MM-DD HH:MM' otherwise. Returns '-' for None.
    """
    if dt is None:
        return "-"
    now = datetime.now(tz=dt.tzinfo)
    today = now.date()
    yesterday = today - timedelta(days=1)
    time_str = dt.strftime("%H:%M")

    if dt.date() == today:
        return i18n.get("date_today_at", time=time_str)
    if dt.date() == yesterday:
        return i18n.get("date_yesterday_at", time=time_str)
    return dt.strftime("%Y-%m-%d %H:%M")
