import rich.cells as _rc

_orig_get_character_cell_size = _rc.get_character_cell_size


# Rich's cell width table doesn't cover U+10EEEE (TGP placeholder,
# Private Use Area plane 16), so the char is measured as 0 cells,
# causing Textual to over-pad rows that contain a TGP rendered image
# and push borders rightward on those rows.
def _patched_get_character_cell_size(
    character: str, unicode_version: str = "auto"
) -> int:
    if ord(character) == 0x10EEEE:
        return 1
    return _orig_get_character_cell_size(character, unicode_version)


_rc.get_character_cell_size = _patched_get_character_cell_size
_rc.cached_cell_len.cache_clear()
